/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
 
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
              $(':input[name="' + hidden_field_name + '"]').removeAttr("value");
            }
        });
        
        $(this).autocomplete({ 
            source: function(request,response){ 
                    $.ajax({ 
                      url: autocompleteurl ,//+ request.term ,
                      dataType: "xml",
                      data: values(), //{ maxRows: 12, q: request.term ,f: values }, 
                      success:   function(xmlresponse){ 
                                    response( $('entry',xmlresponse).map(  function(){ 
                                        return { label: $(this).attr("name"), value:  $(this).attr("name") , id: $(this).attr("id") };
                                    }));    
                          }
                      });
                    },
                select: function(event, ui){
                                    $(this).attr("value",ui.item.value);
                                    $('input[name="' + hidden_field_name + '"]').attr("value",ui.item.id);
                                    if (onAutoCompleteSelectUrl){
                                            $.ajax({
                                              url : onAutoCompleteSelectUrl, 
                                              dataType: "json", 
                                              data: values(), 
                                              success: function(jsonresponse){
                                                for (var i in jsonresponse){
                                                    $(':input[name="' + modelName + '[' + rowIndex + '].' + i +  '"]').attr("value",jsonresponse[i]); 
                                                }
                                              }
                                            });

                                    }
                                }
        }) ;
  });
});

$(function() {
  $('form:first *:input[type!=hidden]:not([disabled]):not([readonly]):first').focus();


  $("a[name='_SUBMIT_NO_MORE']").click(function (){ 
                            $(":submit[name='_SUBMIT_NO_MORE']").trigger('click');
                        });
  $("a[name='_SUBMIT_MORE']").click(function (){ 
                            $(":submit[name='_SUBMIT_MORE']").trigger('click');
                        });
});



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
  $(':input').bind("change", function() {
      setConfirmUnload(true);
  });
  $('form').submit(function(){
      setConfirmUnload(false); 
      return true;
  });
});

