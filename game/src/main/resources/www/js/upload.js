+function($) {
	'use strict';

	// UPLOAD CLASS DEFINITION
	// ======================

	var dropZone = document.getElementById('drop-zone');
	var uploadForm = document.getElementById('js-upload-form');
	var resultList = document.getElementById('result-list');

	var error = function(filename) {
		return function(jqXHR, status, error) {
			$(resultList)
					.prepend(
							$('<a href="#" class="list-group-item list-group-item-danger">'
									+ filename
									+ '<span class="badge pull-right alert-danger">' + jqXHR.responseText + '</span>'
									//+ '<span class="badge alert-failure pull-right">Failure</span>'
									+ '</a>'));
		}
	}

	var success = function(filename) {
		return function(data, status) {
			$(resultList)
					.prepend(
							$('<a href="#" class="list-group-item list-group-item-success">'
									+ filename
									+ '<span class="badge pull-right alert-success">' + data + '</span>'
									//+ '<span class="badge alert-success pull-right">Success</span>'
									+ '</a>'));
		}
	}

	var startUpload = function(files) {
		$.each(files, function(idx, file) {
			var data = new FormData();
			data.append("image", file);

			$.ajax({
				url : '/images/new',
				type : 'POST',
				data : data,
				cache : false,
				dataType : 'json',
				processData : false,
				contentType : false,
				success : success(file.name),
				error : error(file.name)
			});
		});
	}

	uploadForm.addEventListener('submit', function(e) {
		var uploadFiles = document.getElementById('js-upload-files').files;
		e.preventDefault()

		startUpload(uploadFiles)
	})

	dropZone.ondrop = function(e) {
		e.preventDefault();
		this.className = 'upload-drop-zone';

		startUpload(e.dataTransfer.files)
	}

	dropZone.ondragover = function() {
		this.className = 'upload-drop-zone drop';
		return false;
	}

	dropZone.ondragleave = function() {
		this.className = 'upload-drop-zone';
		return false;
	}

}(jQuery);