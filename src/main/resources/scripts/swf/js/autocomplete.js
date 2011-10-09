/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
 
 /*TODO Multiple fields with autocomplete doesnot work */
 
$(function(){
    $("[autocompleteurl]").each(function(index,element){
    	var autocompleteurl = $(this).attr("autocompleteurl");
		var name=$(this).attr("name");
		var hidden_field_name=name.substring("_AUTO_COMPLETE_".length, name.length);
	    $(this).autocomplete({ 
	        source: function(request,response){ 
	                $.ajax({ 
	                  url: autocompleteurl + request.term ,
	                  dataType: "xml",
	                  data: { maxRows: 12, q: request.term }, 
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

