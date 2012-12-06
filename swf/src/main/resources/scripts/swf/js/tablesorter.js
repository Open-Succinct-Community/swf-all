$(function(){
	$(".tablesorter").each(function(index){
    var column= $(this).attr("sortby");
    var order= $(this).attr("order");

    if (!order) {
      order = 0;
    }

    /*
    if (!column) { 
      column = 0;
    }*/

    if (!column){
            $(this)
              .tablesorter({ widthFixed: true, dateFormat: 'uk', widgets: ['zebra'] });
    } else {
            $(this)
              .tablesorter({ widthFixed: true, dateFormat: 'uk', widgets: ['zebra'] , sortList: [[column,order]] });
    }

    });
});
