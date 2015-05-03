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
    $("[autocompleteurl]").each(function(index,element){
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


                //:not([name^="_AUTO"])')
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
                    $(this).attr("value",item.value);
                    $('input[name="' + hidden_field_name + '"]').attr("value",item.id);
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
                  }
                  ajax : { 
                      url: autocompleteurl ,//+ request.term ,
                      method: "get",
                      triggerLength : 1,
                      loadingClass: "loading-circle",
                      preDispatch : function(query){ 
                            showLoadingMask(true);
                            return { search : values() }
                          }
                      preProcess :  function(xmlresponse){ 
                                    response( $('entry',xmlresponse).map(function(){ 
                                        return { label: $(this).attr("name"), value:  $(this).attr("name") , id: $(this).attr("id") };
                                    }));}
                      }
                  }) ;
});

/* Focus on first editable field. */
$(function() {
  $('form:first *:input[type!=hidden]:not([disabled]):not([readonly]):first').focus();


  $("a[name='_SUBMIT_NO_MORE']").click(function (){ 
                            $(":submit[name='_SUBMIT_NO_MORE']").trigger('click');
                        });
  $("a[name='_SUBMIT_MORE']").click(function (){ 
                            $(":submit[name='_SUBMIT_MORE']").trigger('click');
                        });
});

/* Set DatePicker for dateboxes */
$(function(){
    $(".datebox").each(function(index){
        $(this).datetimepicker({format: 'DD/MM/YY'});
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
                      .tablesorter({ widthFixed: true, dateFormat: 'uk', widgets: ['zebra'] , 
                                      textExtraction: textExtractor
                             });
            } else {
                    $(this)
                      .tablesorter({ widthFixed: true, dateFormat: 'uk', widgets: ['zebra'] , sortList: [[column,order]] ,
                                      textExtraction: textExtractor});
            }
    });

});

