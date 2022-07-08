function scrollFunction() {
	if (document.body.scrollTop > 60 || document.documentElement.scrollTop > 60) {
		$("#btnup").show();
	} else {
		$("#btnup").hide();
	}
}

// When the user clicks on the button, scroll to the top of the document
function topFunction() {
	$("html, body").animate({
		scrollTop: 0
	}, 800);
}

function filter(element, what) {
	var value = $(element).val().toLowerCase();

	if (value == '') {
		$('#' + what + ' > li').show();
	}
	else {
		$('#' + what + ' > li').filter(function() {
			let item = $(this).text().toLowerCase().indexOf(value) > -1;
			$(this).toggle(item);
		});
	}
}

/**
 * Module for displaying "Waiting for..." dialog using Bootstrap
 *
 * @author Eugene Maslovich <ehpc@em42.ru>
 * https://bootsnipp.com/snippets/rga0j
 */
var waitingDialog = waitingDialog || (function ($) {
    'use strict';

	// Creating modal dialog's DOM
	var $dialog = $(
		'<div class="modal fade" data-backdrop="static" data-keyboard="false" tabindex="-1" role="dialog" aria-hidden="true" style="padding-top:15%; overflow-y:visible;">' +
		'<div class="modal-dialog modal-m">' +
		'<div class="modal-content">' +
			'<div class="modal-header"><h4 style="margin:0;"></h4></div>' +
			'<div class="modal-body">' +
				'<div class="progress" style="margin-bottom:0;"><div class="progress-bar" style="width: 100%"></div></div>' +
			'</div>' +
		'</div></div></div>');

	return {
		/**
		 * Opens our dialog
		 * @param message Custom message
		 * @param options Custom options:
		 * 				  options.dialogSize - bootstrap postfix for dialog size, e.g. "sm", "m";
		 * 				  options.progressType - bootstrap postfix for progress bar type, e.g. "success", "warning".
		 */
		show: function (message, options) {
			// Assigning defaults
			if (typeof options === 'undefined') {
				options = {};
			}
			if (typeof message === 'undefined') {
				message = 'Loading';
			}
			var settings = $.extend({
				dialogSize: 'm',
				progressType: '',
				onHide: null // This callback runs after the dialog was hidden
			}, options);

			// Configuring dialog
			$dialog.find('.modal-dialog').attr('class', 'modal-dialog').addClass('modal-' + settings.dialogSize);
			$dialog.find('.progress-bar').attr('class', 'progress-bar');
			if (settings.progressType) {
				$dialog.find('.progress-bar').addClass('progress-bar-striped progress-bar-' + settings.progressType);
			}
			$dialog.find('h4').text(message);
			// Adding callbacks
			if (typeof settings.onHide === 'function') {
				$dialog.off('hidden.bs.modal').on('hidden.bs.modal', function (e) {
					settings.onHide.call($dialog);
				});
			}
			// Opening dialog
			$dialog.modal();
		},
		/**
		 * Closes dialog
		 */
		hide: function () {
			$dialog.modal('hide');
		}
	};

})(jQuery);


// highligth.js (like initHighlightingOnLoad)
document.addEventListener('DOMContentLoaded', (event) => {
	document.querySelectorAll('pre>code[class*="prettyprint"]').forEach((block) => {
		hljs.highlightBlock(block);
	});
});
// add HighlightJS-badge (options are optional)
$(document).ready(function() {
	// add HighlightJS-badge (options are optional)
	var options = {
		contentSelector : 'pre[class*="code-badge-pre"]',
		label : 'Copia',
		title : 'Copia',
		hasLineNumber : 'false',
		badgeClickable : 'true',
		xmlBeautifier : 'true'
	};
	window.highlightJsBadge(options);
});
