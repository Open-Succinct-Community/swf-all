/* Unsaved data Warning Code */
function setConfirmUnload(on) {
     window.onbeforeunload = (on) ? unloadMessage : null;
}

function unloadMessage(e) {
     return ' If you navigate away from this page without' +
        ' first saving your data, the changes will be' +
        ' lost.';
}

$(function(){
  $(':input:not([name="q"])').bind("change", function() {
      setConfirmUnload(true);
  });
  $('form').submit(function(){
      setConfirmUnload(false);
      return true;
  });
});

$(function(){
    $("[autocompleteurl]:not([readonly])").each(function(index,element){
        var autocompleteurl = $(this).attr("autocompleteurl");
        var onAutoCompleteSelectUrl = $(this).attr("onAutoCompleteSelectUrl");

        var name=$(this).attr("name");

        var bIsNameIndexed = false;

        var nameParts=name.split("[");
        var modelName = "";
        var rowIndex = "" ;

        if (nameParts.length == 2){
          modelName = nameParts[0];
          rowIndex = nameParts[1].split("]")[0]
          bIsNameIndexed = true;
        }
        var hidden_field_name = name.replace("_AUTO_COMPLETE_","");

        var values = (function(){
                var v = {} ;

                var $inputs = null;
                if ( !bIsNameIndexed ) {
                  $inputs = $(':input[name$="ID"]');
                }else {
                  $inputs = $(':input[name^="'                 + modelName + '[' + rowIndex + ']."][name$="ID"]')
                }


                $inputs.each(function() {
                  if (this.name == name ||
                      this.name.indexOf("_AUTO_COMPLETE_") < 0 ){

                    var cname = this.name;

                    if (bIsNameIndexed) {
                      cname = this.name.split(".")[1];
                    }

                    v[cname] = $(this).val();
                  }
                });

                return v;
        }) ;

        $(this).focusout(function(){
            if ( $(this).val().length == 0 ) {
              target=$(':input[name="' + hidden_field_name + '"]');
              hidden_value=target.attr("value");
              if (hidden_value) {
                target.removeAttr("value");
                setConfirmUnload(true)
              }
            }
        });

        $(this).typeahead({
                onSelect: function(item){
                    // Input has text and value instead of name and id!!
                    //$(this).attr("value",item.text);
                    $('input[name="' + hidden_field_name + '"]').attr("value",item.value);
                    setConfirmUnload(true)
                    if (onAutoCompleteSelectUrl){
                            $.ajax({
                              url : onAutoCompleteSelectUrl,
                              dataType: "json",
                              data: values(),
                              success: function(jsonresponse){
                                for (var i in jsonresponse){
                                    if ( bIsNameIndexed ) {
                                      $(':input[name="' + modelName + '[' + rowIndex + '].' + i +  '"]').attr("value",jsonresponse[i]);
                                    }else {
                                      $(':input[name="' + i +  '"]').attr("value",jsonresponse[i]);
                                    }
                                    setConfirmUnload(true)
                                }
                              }
                            });

                    }
                  },
                  matcher:function(item){
                        let firstword = this.query;
                        let words = this.query.split(" ");
                        if (words.length > 0 ){
                            firstword = words[0] ;
                        }
                        firstword = firstword.toLowerCase();
                        i = item.toLowerCase();
                        return ~i.indexOf(firstword) || ~i.indexOf(firstword.replace("%",""))
                                || ~i.indexOf(firstword.replace("*",""))
                  },
                  ajax : {
                      url: autocompleteurl ,//+ request.term ,
                      method: "get",
                      triggerLength : 1,
                      loadingClass: "loading-circle",
                      preDispatch : function(query){
                            if (query == "*" || query == "%" ) {
                              field = $(':input[name="'+name+'"]');
                              field.val("");
                            }
                            return  values();
                          },
                      preProcess :  function(json){
                                      return json.entries;
                                    }
                  }

        }) ;
    });
});

/* Focus on first editable field. */
$(function() {
  $('form:first *:input[type!=hidden]:not([disabled]):not([readonly]):not(.btn):first').focus();


  $("a[name='_SUBMIT_NO_MORE']").click(function (){
                            $(":submit[name='_SUBMIT_NO_MORE']").trigger('click');
                        });
  $("a[name='_SUBMIT_MORE']").click(function (){
                            $(":submit[name='_SUBMIT_MORE']").trigger('click');
                        });
});

/* Set DatePicker for dateboxes */
$(function(){
    $(".date-box").each(function(index){
        /*
        var date = moment($(this).val(),'YYYY-MM-DD');

        $(this).datetimepicker({format: 'YYYY-MM-DD'})
        .on('change',function(ev){
            $(this).valid();
        }) ;
        $(this).data("DateTimePicker").date(date);
        */
        $(this).datetimepicker({format: 'YYYY-MM-DD',allowInputToggle : true});
    });
});
$(function(){
    $(".timestamp-box").each(function(index){
        /*
        var date = moment($(this).val(),'YYYY-MM-DD');

        $(this).datetimepicker({format: 'YYYY-MM-DD'})
        .on('change',function(ev){
            $(this).valid();
        }) ;
        $(this).data("DateTimePicker").date(date);
        */
        $(this).datetimepicker({format: 'DD/MM/YYYY HH:mm:00',allowInputToggle : true});
    });
});

/* Table Sorter */
$(function(){
    var textExtractor = function(elem){
                          var $input = $("input[type=text]",elem);
                          var $label = $("label",elem);
                          if ($input.length == 1) {
                            return $input.val();
                          }else if ($label.length == 1){
                            return $label.text();
                          }else{
                            return $(elem).text();
                          }
                        }

	  $(".tablesorter").each(function(index){
            var column= $(this).attr("sortby");
            var order= $(this).attr("order");

            if (!order) {
              order = 0;
            }


            if (!column){
                    $(this)
                      .tablesorter({ theme: 'bootstrap', widthFixed: false , dateFormat: 'uk', widgets: ['zebra','uitheme',  'scroller'] ,
                                    widgetOptions: {
                                          zebra : [ "odd", "even" ] ,
                                          scroller_rowHighlight : 'hover',
                                          scroller_height : 300
                                    },
                                    headerTemplate: '{content} {icon}',
                                      textExtraction: textExtractor
                             });
            } else {
                    $(this)
                      .tablesorter({ theme: 'bootstrap', widthFixed: false , dateFormat: 'uk', widgets: ['zebra','uitheme',  'scroller' ] ,
                                    widgetOptions: {
                                          zebra : [ "odd", "even" ] ,
                                          scroller_rowHighlight : 'hover',
                                          scroller_height : 300
                                    },
                                      headerTemplate: '{content} {icon}',
                                      sortList: [[column,order]] , textExtraction: textExtractor});
            }
    });

});

/* File upload button as bootstrap */
$(document).on('change', '.btn-file :file', function () {
    var input = $(this), numFiles = input.get(0).files ? input.get(0).files.length : 1, label = input.val().replace(/\\/g, '/').replace(/.*\//, '');
    input.trigger('fileselect', [
        numFiles,
        label
    ]);
});
$(document).ready(function () {
    $('.btn-file :file').on('fileselect', function (event, numFiles, label) {
        var input = $(this).parents('.input-group').find(':text'), log = numFiles > 1 ? numFiles + ' files selected' : label;
        if (input.length) {
            input.val(log);
        } else {
            if (log)
                alert(log);
        }
    });
});

function api() {
    var _url ;
    var _parameter;
    var _headers ;

    return {
        url : function(url){
            if (arguments.length == 0){
                return _url;
            }
            _url = encodeURIComponent(url);
            return this;
        },
        parameters : function(p){
            if (!_parameter){
                _parameter = {};
            }
            if (arguments.length == 0){
                return _parameter;
            }
            if (p.constructor == new FormData().constructor){
                var object = {};
                p.forEach(function(value, key){
                    object[key] = value;
                });
                _parameter = object;
            }else {
                _parameter = p;
            }
            return this;
        },
        headers : function(additional_headers){
            defaultOptions = { 'Content-Type': 'application/json' , 'Cache-Control': 'no-cache' };
            if (!_headers){
                _headers = {} ;
            }
            _headers = { ..._headers ,  ...defaultOptions}
            if (arguments.length > 0){
                _headers = { ..._headers, ...additional_headers } ;
                return this;
            }
            return _headers;
        },
        http : function(){
            return axios.create({
                baseURL : "/",
                timeout : 30000 ,
                auth : { username : "web"  , password : "venky12" },
                headers : { 'Content-Type': 'application/json' , 'Cache-Control': 'no-cache' },
                withCredentials: false,
            });
        },
        get : function(){
            let self = this;
            return self.http().get(self.url(),{ data :{} , "headers": self.headers() }).then(function(response){
                return response.data;
            });
        },
        post : function(){
            let self = this;
            return self.http().post(self.url(),  _parameter  , { "headers" :  self.headers() } ).then(function(response){
                return response.data;
            });
        }
    }
}
