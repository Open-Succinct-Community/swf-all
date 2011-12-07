$(function(){
	$(".tablesorter").each(function(index){
		var pagerId = $(this).attr("pagerid");
		var pager = $("#"+pagerId);
		$(this)
			.tablesorter({ widthFixed: true, dateFormat: 'uk', widgets: ['zebra'] , sortList: [[0,0],[1,0],[2,0],[3,0]] });
    });
});
