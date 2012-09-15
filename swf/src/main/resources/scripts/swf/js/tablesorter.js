$(function(){
	$(".tablesorter").each(function(index){
    var column= $(this).attr("sortby");
    var order= $(this).attr("order");
    if (!column) { 
      column = 0;
    }
    if (!order) {
      order = 0;
    }
		$(this)
			.tablesorter({ widthFixed: true, dateFormat: 'uk', widgets: ['zebra'] , sortList: [[column,order]] });
    });
});
