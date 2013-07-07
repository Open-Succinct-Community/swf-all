function GetURLParameter(sParam){
    var sPageURL = window.location.search.substring(1);
    var sURLVariables = sPageURL.split('&');
    for (var i = 0; i < sURLVariables.length; i++) 
    {
        var sParameterName = sURLVariables[i].split('=');
        if (sParameterName[0] == sParam) 
        {
            return sParameterName[1];
        }
    }
}

$(function(){
    var _findex = 0; 
    var tabName = GetURLParameter("_select_tab");
    var textExtractor = function(elem){
                          var $label = $("label",elem);
                          var $input = $("input[type=text]",elem);
                          if ($input) {
                            return $input.val();
                          }else {
                            return $label.text();
                          }
                        }

    $(".tabs ul li a").each(function(i){
        if ( $(this).text() == tabName ) {
          _findex = i ;
          return false;
        }
        return true;
    });
    $(".tabs").tabs({active: _findex});

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

