$(function(){
	$(".tablesorter").each(function(index){
		var pagerId = $(this).attr("pagerid");
		var pager = $("#"+pagerId);
		$(this)
			.tablesorter({ widthFixed: true, dateFormat: 'uk', widgets: ['zebra']})
			.tablesorterPager({ container: pager })	;
    });
});
