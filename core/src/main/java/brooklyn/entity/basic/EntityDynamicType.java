package brooklyn.entity.basic;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.config.ConfigKey;
import brooklyn.config.ConfigKey.HasConfigKey;
import brooklyn.entity.Effector;
import brooklyn.entity.Entity;
import brooklyn.entity.EntityType;
import brooklyn.event.Sensor;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

public class EntityDynamicType {

    // TODO Deal with Serializable: what is the requirement? It depends on how we implement remoting,
    // so deferring it for now...
    
    protected static final Logger LOG = LoggerFactory.getLogger(EntityDynamicType.class);

    private final AbstractEntity entity;

    /** 
     * Effectors on this entity.
     * TODO support overloading; requires not using a map keyed off method name.
     */
    private final ConcurrentMap<String, Effector<?>> effectors = new ConcurrentHashMap<String, Effector<?>>();

    /** 
     * Map of sensors on this entity by name.
     */
    private final ConcurrentMap<String,Sensor<?>> sensors = new ConcurrentHashMap<String, Sensor<?>>();

    /** 
     * Map of config keys on this entity by name.
     */
    private final ConcurrentMap<String,ConfigKey<?>> configKeys = new ConcurrentHashMap<String, ConfigKey<?>>();

    private volatile EntityTypeSnapshot snapshot;
    private final AtomicBoolean snapshotValid = new AtomicBoolean(false);
    
    public EntityDynamicType(AbstractEntity entity) {
        this.entity = entity;
        
        effectors.putAll(findEffectors(entity));
        if (LOG.isTraceEnabled())
            LOG.trace("Entity {} effectors: {}", entity.getId(), Joiner.on(", ").join(effectors.keySet()));
        
        sensors.putAll(findSensors(entity));
        if (LOG.isTraceEnabled())
            LOG.trace("Entity {} sensors: {}", entity.getId(), Joiner.on(", ").join(sensors.keySet()));
        
        configKeys.putAll(findConfigKeys(entity));
        if (LOG.isTraceEnabled())
            LOG.trace("Entity {} config keys: {}", entity.getId(), Joiner.on(", ").join(configKeys.keySet()));

        refreshSnapshot();
    }
    
    public synchronized EntityType getSnapshot() {
        return refreshSnapshot();
    }
    
    /**
     * @return the effector with the given name, or null if not found
     */
    public Effector<?> getEffector(String name) {
        return effectors.get(name);
    }
    
    /**
     * Effectors available on this entity.
     *
     * NB no work has been done supporting changing this after initialization,
     * but the idea of these so-called "dynamic effectors" has been discussed and it might be supported in future...
     */
    public Map<String,Effector<?>> getEffectors() {
        return Collections.unmodifiableMap(effectors);
    }
    
    /**
     * Sensors available on this entity.
     */
    public Map<String,Sensor<?>> getSensors() {
        return Collections.unmodifiableMap(sensors);
    }
    
    /** 
     * Convenience for finding named sensor.
     */
    public Sensor<?> getSensor(String sensorName) {
        return sensors.get(sensorName);
    }

    /**
     * ConfigKeys available on this entity.
     */
    public Map<String,ConfigKey<?>> getConfigKeys() {
        return Collections.unmodifiableMap(configKeys);
    }

    /**
     * Adds the given {@link Sensor} to this entity.
     */
    public void addSensor(Sensor<?> newSensor) {
        sensors.put(newSensor.getName(), newSensor);
        snapshotValid.set(false);
        entity.emit(AbstractEntity.SENSOR_ADDED, newSensor);
    }
    
    /**
     * Adds the given {@link Sensor}s to this entity.
     */
    public void addSensors(Iterable<? extends Sensor<?>> newSensors) {
        for (Sensor<?> sensor : newSensors) {
            addSensor(sensor);
        }
    }
    
    public void addSensorIfAbsent(Sensor<?> newSensor) {
        Sensor<?> prev = sensors.putIfAbsent(newSensor.getName(), newSensor);
        if (prev == null) {
            snapshotValid.set(false);
            entity.emit(AbstractEntity.SENSOR_ADDED, newSensor);
        }
    }

    /**
     * Removes the named {@link Sensor} from this entity.
     */
    public Sensor<?> removeSensor(String sensorName) {
        Sensor<?> result = sensors.remove(sensorName);
        if (result != null) {
            snapshotValid.set(false);
            entity.emit(AbstractEntity.SENSOR_REMOVED, result);
        }
        return result;
    }
    
    /**
     * Removes the named {@link Sensor} from this entity.
     */
    public boolean removeSensor(Sensor<?> sensor) {
        return (removeSensor(sensor.getName()) != null);
    }
    
    /**
     * ConfigKeys available on this entity.
     */
    public ConfigKey<?> getConfigKey(String keyName) { 
        return configKeys.get(keyName); 
    }
    
    private EntityTypeSnapshot refreshSnapshot() {
        if (snapshotValid.compareAndSet(false, true)) {
            snapshot = new EntityTypeSnapshot(entity.getClass().getCanonicalName(), configKeys, 
                    sensors, effectors.values());
        }
        return snapshot;
    }
    
    /**
     * Finds the effectors sensors defined on the entity's class.
     */
    private static Map<String,Effector<?>> findEffectors(Entity entity) {
        try {
            Class<? extends Entity> clazz = entity.getClass();
            Map<String,Effector<?>> result = Maps.newLinkedHashMap();
            for (Field f : clazz.getFields()) {
                if (Effector.class.isAssignableFrom(f.getType())) {
                    Effector<?> eff = (Effector<?>) f.get(entity);
                    Effector<?> overwritten = result.put(eff.getName(), eff);
                    if (overwritten!=null && overwritten != eff) 
                        LOG.warn("multiple definitions for effector {} on {}; preferring {} to {}", new Object[] {eff.getName(), entity, eff, overwritten});
                }
            }
            
            return result;
        } catch (IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
    }
    
    /**
     * Finds the sensors statically defined on the entity's class.
     */
    private static Map<String,Sensor<?>> findSensors(Entity entity) {
        try {
            Class<? extends Entity> clazz = entity.getClass();
            Map<String,Sensor<?>> result = Maps.newLinkedHashMap();
            for (Field f : clazz.getFields()) {
                if (Sensor.class.isAssignableFrom(f.getType())) {
                    Sensor<?> sens = (Sensor<?>) f.get(entity);
                    Sensor<?> overwritten = result.put(sens.getName(), sens);
                    if (overwritten!=null && overwritten != sens) 
                        LOG.warn("multiple definitions for sensor {} on {}; preferring {} to {}", new Object[] {sens.getName(), entity, sens, overwritten});
                }
            }

            return result;
        } catch (IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
    }
    
    /**
     * Finds the config keys statically defined on the entity's class.
     */
    private static Map<String,ConfigKey<?>> findConfigKeys(Entity entity) {
        try {
            Class<? extends Entity> clazz = entity.getClass();
            Map<String,ConfigKey<?>> result = Maps.newLinkedHashMap();
            Map<String,Field> configFields = Maps.newLinkedHashMap();
            for (Field f : clazz.getFields()) {
                ConfigKey<?> k = null;
                if (ConfigKey.class.isAssignableFrom(f.getType())) {
                    k = (ConfigKey<?>) f.get(entity);
                } else if (HasConfigKey.class.isAssignableFrom(f.getType())) {
                    k = ((HasConfigKey<?>)f.get(entity)).getConfigKey();
                }
                if (k != null) {
                    Field alternativeField = configFields.get(k.getName());
                    // Allow overriding config keys (e.g. to set default values) when there is an assignable-from relationship between classes
                    Field definitiveField = alternativeField != null ? inferSubbestField(alternativeField, f) : f;
                    boolean skip = false;
                    if (definitiveField != f) {
                        // If they refer to the _same_ instance, just keep the one we already have
                        if (alternativeField.get(entity) == f.get(entity)) skip = true;
                    }
                    if (skip) {
                        //nothing
                    } else if (definitiveField == f) {
                        ConfigKey<?> overwritten = result.put(k.getName(), k);
                        configFields.put(k.getName(), f);
                    } else if (definitiveField != null) {
                        if (LOG.isDebugEnabled()) LOG.debug("multiple definitions for config key {} on {}; preferring that in sub-class: {} to {}", new Object[] {k.getName(), entity, alternativeField, f});
                    } else if (definitiveField == null) {
                        LOG.warn("multiple definitions for config key {} on {}; preferring {} to {}", new Object[] {k.getName(), entity, alternativeField, f});
                    }
                }
            }
            
            return result;
        } catch (IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
    }
    
    /**
     * Gets the field that is in the sub-class; or null if one field does not come from a sub-class of the other field's class
     */
    private static Field inferSubbestField(Field f1, Field f2) {
        Class<?> c1 = f1.getDeclaringClass();
        Class<?> c2 = f2.getDeclaringClass();
        boolean isSuper1 = c1.isAssignableFrom(c2);
        boolean isSuper2 = c2.isAssignableFrom(c1);
        return (isSuper1) ? (isSuper2 ? null : f2) : (isSuper2 ? f1 : null);
    }
}