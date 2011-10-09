$(function(){
	$(".tablesorter").each(function(index){
		$(this)
			.tablesorter({ widthFixed: true, dateFormat: 'uk', widgets: ['zebra']})
			.tablesorterPager({ container: $("#pager") })	;
    });
});
