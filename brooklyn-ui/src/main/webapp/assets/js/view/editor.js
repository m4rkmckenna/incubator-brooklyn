/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
*/
define([
    "underscore", "jquery", "backbone", "model/catalog-application", "codemirror",
    "text!tpl/editor/page.html",

    // no constructor
    "jquery-slideto",
    "jquery-wiggle",
    "jquery-ba-bbq",
    "handlebars",
    "bootstrap"
], function (_, $, Backbone, CatalogApplication, CodeMirror, EditorHtml) {
    var _DEFAULT_BLUEPRINT = 'name: Brooklyn App\nlocation: localhost\nservices:\n- type: com.example.Entity';

    var EditorView = Backbone.View.extend({
        tagName:"div",
        className:"container container-fluid",
        events: {
            'click #button-run':'runBlueprint',
            'click #button-delete':'removeBlueprint',
        },
        editorTemplate:_.template(EditorHtml),

        editor: null,

        initialize:function () {
            console.log("initialize");
            var vm = this;
            this.options.catalog = new CatalogApplication.Collection();
            this.options.catalog.fetch({
                data: $.param({allVersions: true}),
                success: function () {
                    vm.refreshEditor();
                }
            });
            /* this.editor = CodeMirror.fromTextArea(document.getElementById("user"), {
                lineNumbers: true,
                extraKeys: {"Ctrl-Space": "autocomplete"},
                // TODO: feature request: to allow custom theme: http://codemirror.net/demo/theme.html#base16-light
                mode: {
                    name: "yaml",
                    globalVars: true
                }
            }); */
        },
        render:function (eventName) {
            log(document.getElementById("yaml_code"));
            this.$el.html(_.template(EditorHtml, {}));
            this.loadEditor();
            log("render this:", this);
            return this;
        },
        refreshEditor: function() {
            log("this: ", this);
            var cm = this.editor;
            if (typeof(cm) !== "undefined") {
                var item = this.options.catalog.getId(this.id);
                if(item){
                    cm.getDoc().setValue(item['attributes']['planYaml']);
                }else{
                    cm.getDoc().setValue(_DEFAULT_BLUEPRINT);
                }
                log("CodeMirror", cm);
                // cm.ensureFocus();
                //cm.setCursor(cm.lineCount(), 0);
                cm.focus();
                cm.refresh();
                // set cursor to End of Document

            }

        },
        loadEditor: function() {
            log("loadEditor:", this);
            log( this.$("#yaml_code")[0] );
            if (this.editor == null) {
                log("creating editor")
                this.editor = CodeMirror.fromTextArea(this.$("#yaml_code")[0], {
                    lineNumbers: true,
                    extraKeys: {"Ctrl-Space": "autocomplete"},
                    // TODO: feature request: to allow custom theme: http://codemirror.net/demo/theme.html#base16-light
                    mode: {
                        name: "yaml",
                        globalVars: true
                    }
                });
            }

            this.refreshEditor();
        },
        validate: function() {
            log("validate");
            return true;
        },
        runBlueprint: function() {
            log("runBlueprint");
            if (this.validate())
                this.submitApplication();
        },
        removeBlueprint: function() {
            this.refreshEditor();

        },
        onSubmissionComplete: function(succeeded, data) {
            log("[onSubmissionComplete] succeeded:", succeeded);
            log("[onSubmissionComplete] data:", data);
            var that = this;
            if(succeeded){
                Backbone.history.navigate('v1/home' ,{trigger: true});
            }else{
                log("ERROR submitting application: "+data.responseText);
                var response, summary="Server responded with an error";
                try {
                    if (data.responseText) {
                        response = JSON.parse(data.responseText)
                        if (response) {
                            summary = response.message;
                        }
                    }
                } catch (e) {
                    summary = data.responseText;
                }
                that.showFailure(summary);
            }
            //    that.steps[that.currentStep].view.showFailure(summary)
        },
        submitApplication:function (event) {
            log("submitApplication");
            var that = this

            $.ajax({
                url:'/v1/applications',
                type:'post',
                contentType:'application/yaml',
                processData:false,
                data: that.editor.getValue(),
                success:function (data) {
                    that.onSubmissionComplete(true, data)
                },
                error:function (data) {
                    that.onSubmissionComplete(false, data)
                }
            });

            return false
        },
        showFailure: function(text) {
            if (!text) text = "Failure performing the specified action";
            log("showing error: "+text);
            this.$('div.error-message .error-message-text').html(_.escape(text));
            // flash the error, but make sure it goes away (we do not currently have any other logic for hiding this error message)
            this.$('div.error-message').slideDown(250).delay(10000).slideUp(500);
        }
    });

    return EditorView;
});
