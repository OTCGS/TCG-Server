
function load_edition_names(next) {
	$.get('/cardDatas/editions', function(data) {
		var select = $('#id_edition_select');
		for ( var idx in data) {
			select.append($('<option>' + data[idx] + '</option>'));
		}
		if (next != null) {
			next();
		}
	});
}

function load_images() {
	$.get('/images', function(data) {
		var select = $('#id_image_select');
		for ( var idx in data) {
			select.append($('<option data-img-src="/images/' + data[idx] + '/thumb">' + data[idx] + '</option>'));
		}
		select.imagepicker();
		var list = $(".image_picker_selector");
		imagesLoaded(list, function() {
			list.masonry({
				itemSelector : '.thumbnail',
				columnWidth : 80,
				gutter : 5
			});
		});
	});
}

function setup_edition_select() {
	var enter_edition = $('<option disabled>Enter edition name...</option>');
	enter_edition.on('click', function() {
		$('#id_modal_dialog').modal({});
	});
	$('#id_edition_select').append(enter_edition);

	$('#id_modal_dialog_ok_button').on('click', function() {
		var value = $('#id_new_edition_name').val();
		$('#id_edition_select').append($('<option>' + value + '</option>'));
		$('#id_new_edition_name').val('');
		$('#id_edition_select').val(value);
		$('#id_modal_dialog').modal('hide');
	});
	$('#id_modal_dialog').on('shown.bs.modal', function() {
		$('#id_new_edition_name').focus();
	})
}

function create_key_value_dom_elements(key, value) {
	var delete_button = $('<input class="form-control btn-danger" type="button" value="X" />');
	var key_field = $('<input class="form-control" type="text" value="' + key + '" name="key" placeholder="Key" />')
	var value_field = $('<input class="form-control" type="text" value="' + value
		+ '" name="value" placeholder="Value" />')
	var new_row = $('<div class="key-value-row form-group" ><div class="col-xs-2" /></div>');
	var div_key = $('<div class="col-xs-4"></div>');
	div_key.append(key_field);
	var div_value = $('<div class="col-xs-4"></div>');
	div_value.append(value_field);
	var div_delete_button = $('<div class="col-xs-1"></div>');
	div_delete_button.append(delete_button);

	$(delete_button).on('click', function() {
		new_row.remove();
	});

	new_row.append(div_key);
	new_row.append(div_value);
	new_row.append(div_delete_button);
	$('#id_key_values').append(new_row);
}

function setup_more_values_button() {
	$('#id_more_values').on('click', function() {
		create_key_value_dom_elements("", "");
	});
}

function setup_revision_buttons(max_revision) {
	var revision = $('#id_card_revision').val();
	$('#id_card_revision_previous').off();
	$('#id_card_revision_previous').on('click', function() {
		set_card_data(parseInt($('#id_card_revision').val()) - 1, max_revision);
	});
	$('#id_card_revision_next').off();
	$('#id_card_revision_next').on('click', function() {
		set_card_data(parseInt($('#id_card_revision').val()) + 1, max_revision);
	});
	$('#id_card_revision_previous').attr('disabled', !(revision > 1));
	$('#id_card_revision_next').attr('disabled', !(revision < max_revision));
}

function setup_image_select() {
	var image_picker_dialog = make_dialog(function success(uuid) {
		$('#id_image').attr('src', '/images/' + uuid + "/thumb");
		$('#id_image').attr('data-uuid', uuid);
	});
	$('#id_image_drop_zone').on('click', function() {
		image_picker_dialog.show();
	});
}

function get_key_values() {
	var key_values = {};
	$('.key-value-row').each(function(idx, elem) {
		var key = $(elem).find('input[name="key"]').val();
		var value = $(elem).find('input[name="value"]').val();
		if (key != undefined && key != "") {
			key_values[key] = value;
		}
	});
	return key_values;
}
function set_card_data(revision, max_revision) {
	var uuid = $('#id_uuid').val();
	var url;
	if (uuid != "") {
		if (revision == "" || revision == undefined) {
			url = "";
		} else {
			url = "/" + revision;
		}
		$.get('/cardDatas/' + uuid + url, function(card) {
			$('#id_card_name').val(card.name);
			$('#id_card_revision').val(card.revision);
			$('#id_edition_select').val(card.edition);
			$('#id_image').attr('src', '/images/' + card.imageId + '/thumb');
			$('#id_image').attr('data-uuid', card.imageId);
			$('#id_key_values').empty();
			var keys = Object.keys(card.values);
			for ( var idx in keys) {
				create_key_value_dom_elements(keys[idx], card.values[keys[idx]]);
			}
			if (max_revision == undefined) {
				max_revision = $('#id_card_revision').val();
			}
			setup_revision_buttons(max_revision);
		});
	}
}

$(document).ready(function() {
	load_images();
	setup_edition_select();
	setup_image_select();
	setup_more_values_button();
	load_edition_names(set_card_data);
	var uuid = $('#id_uuid').val();
	if (uuid != "") {
		$('#id_save_button').val('update');
		$('#id_save_button').on('click', function() {
			$('#id_done').empty();
			$.ajax({
				url : "/cardDatas/" + uuid,
				type : 'PUT',
				data : {
					id : uuid,
					name : $('#id_card_name').val(),
					edition : $('#id_edition_select').val(),
					imageId : $('#id_image').attr('data-uuid'),
					values : JSON.stringify(get_key_values())
				},
				success : function(result) {
					set_card_data();
					$('#id_done').append('<span class="label col-xs-2 col-xs-offset-2 label-success">Success</span>');
				},
				error : function(result) {
					$('#id_done').append('<span class="label label-danger">' + result + '</span>');
				}
			});
		});
	} else {
		create_key_value_dom_elements("", "");
		$('#id_save_button').on('click', function() {
			$('#id_done').empty();
			$.ajax({
				url : "/cardDatas",
				type : 'POST',
				data : {
					name : $('#id_card_name').val(),
					edition : $('#id_edition_select').val(),
					imageId : $('#id_image').attr('data-uuid'),
					values : JSON.stringify(get_key_values())
				},
				success : function(result) {
					window.location = "/edit/card?uuid=" + result.uuid;
				},
				error : function(result) {
					$('#id_done').append('<span class="label label-danger">' + result + '</span>');
				}
			});
		});
	}
});