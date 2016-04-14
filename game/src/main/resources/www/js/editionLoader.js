function set_active(elem) {
	$(elem).addClass(active);
}

function make_list_item(name, id) {
	var button = $('<button type="button" class="btn btn-xs">'
			+ '<span class="glyphicon glyphicon-remove" />' + '</button>');
	var list_item = $('<li class="list-group-item" data-id="'+ id +'">' + name
			+ '<span class="pull-right"></span>' + '</li>');
	button.on('click', function(event) {
		$.ajax({
			url : '/editions/' + id,
			type : 'DELETE',
			success : function(data) {
				list_item.remove();
			}
		});
		event.stopPropagation();
	});
	list_item.children().append(button);
	return list_item;
}

function update_list() {
	var editionList = $('#id_edition_list');
	$.get("/editions", function(data) {
		editionList.empty();
		for ( var idx in data) {
			$.get("/editions/" + data[idx], function(data) {
				editionList.append(make_list_item(data.name, data.uuid));
			});
		}
	});
}

$(document).ready(function() {
	update_list();
	$('#id_create_button').on('click', function(event) {
		$.post("/editions/new", {
			name : $('#id_name').val()
		}, function() {
			update_list();
		});
	});
	$('#id_edition_list').on('click', 'li', function(event) {
		var data_id = $(event.target).attr('data-id');
		$.get("/editions/" + data_id + "/cardNames", function(data) {
			alert(data);
		});
	});
});