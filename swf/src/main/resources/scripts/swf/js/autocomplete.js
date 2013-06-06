/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
 
$(function(){
    $("[autocompleteurl]").each(function(index,element){
        var autocompleteurl = $(this).attr("autocompleteurl");
        var name=$(this).attr("name");
        var hidden_field_name=name.substring("_AUTO_COMPLETE_".length, name.length);
        var values = (function(){
                var v = {} ;
                var $inputs = $('form:first :input[name$="ID"]');
                //:not([name^="_AUTO"])')
                $inputs.each(function() { 
                  if (this.name == name || 
                      this.name.indexOf("_AUTO_COMPLETE_") < 0 ){
                    v[this.name] = $(this).val();
                  }
                });

                return v;
        }) ;
        
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
})


