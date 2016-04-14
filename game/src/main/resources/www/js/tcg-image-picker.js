$('body').append($('<script src="/resources/js/imagesloaded.pkgd.js"></script>'));
$('body').append($('<script src="/resources/js/masonry.pkgd.min.js"></script>'));
$('body').append($('<script src="/resources/js/image-picker.js"></script>'));

function make_dialog(success, cancel) {
	function load_images() {
		$.get('/images', function(data) {
			var select = $('#id_image_select');
			select.empty();
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
	function make_div() {
		return $('<div id="id_modal_dialog" class="modal">' + '<div class="modal-dialog">'
			+ '<div class="modal-content">' + '<div class="modal-header">'
			+ '<button type="button" class="close" data-dismiss="modal"' + ' aria-label="Close">'
			+ '<span aria-hidden="true">&times;</span>' + '</button>' + '<h3 class="modal-title">Select image</h3>'
			+ '</div>' + '<div class="modal-body">' + '<div class="form-group">'
			+ '<input id="id_image_select" type="select" />' + '</div>' + '<div class="form-group">'
			+ '<button id="id_success" type="button" class="btn btn-primary">Done</button>'
			+ '<button id="id_cancel" type="button" class="btn btn-primary danger">Cancel</button>' + '</div>'
			+ '</div>' + '</div>' + '</div>' + '</div>');
	}

	var div = make_div();
	$('body').append(div);
	$('#id_success').on('click', function() {
		if (success != undefined) {
			var select = $('#id_image_select');
			success(select.val());
		}
		$(div).modal('hide');
	});
	$('#id_cancel').on('click', function() {
		if (cancel != undefined)
			cancel();
		$(div).modal('hide');
	});

	var dialog = {
		show : function show() {
			load_images();
			$(div).modal({});
		}
	};
	return dialog;
}
