'use strict';

angular.module('iraj-message-service', [])
	.factory('irajMessageService', function() {
		var irajMessageService = {};
		
		irajMessageService.displayFieldMessages = function(fieldMessages, irajMessagesValue) {
			// fieldMessages = [{id: 'fieldId', message: 'messageToDisplay', type: 'warning' | 'error' | 'info' | 'success'}, ...]
			for (var i = 0; i < fieldMessages.length; i++) {
				irajMessageService.displayFieldMessage(fieldMessages[i], irajMessagesValue);
			}
		};
		
		irajMessageService.displayFieldMessage = function(fieldMessage, irajMessagesValue) {
			var inputEl = jQuery('#'+fieldMessage.id); // search the input field
			if (inputEl.length > 0) {
				// search the help-inline for that input
				var helpEl = inputEl.next('.help-inline');
				if (helpEl.length == 0) {
					// the help inline doesn't exist, let's create it
					helpEl = inputEl.after('<div class="help-inline"/>').next('.help-inline');
				}
				// append the message to it
				helpEl.append(fieldMessage.message);
				// now search the parent "control-group" in order to change the type of warning
				inputEl.parents('.control-group').eq(0)
					.removeClass("warning error info success") // first remove previous messages type
					.addClass(fieldMessage.type);	// and add this type
			} else {
				// the input has not been found, let's append a "normal" message
				irajMessageService.displayMessage({message: fieldMessage.id + ' : ' + fieldMessage.message, type: fieldMessage.type}, irajMessagesValue);
			}
		};
		
		irajMessageService.clearFieldMessages = function(idPrefix) {
			jQuery("[id^='"+idPrefix+"'] ~ .help-inline") // foreach help-inline which has a matching input as previous sibling
				.empty() // clear all text inside
				.parents('.control-group') // search the control-group
				.removeClass("warning error info success"); // and remove the type of message
		};
		
		irajMessageService.displayMessages = function(messages, irajMessagesValue) {
			for (var i = 0 ; i < messages.length ; i++) {
				var message = messages[i];
				irajMessageService.displayMessage(message, irajMessagesValue);
			}
		};
		
		irajMessageService.displayMessage = function(message, irajMessagesValue) {
			var irajMessagesEl = jQuery("[irajMessages^='"+irajMessagesValue+"']"); // find the irajMessages="irajMessagesValue" elements
			if (irajMessagesEl.length > 0) {
				irajMessageService.appendMessageAlertToEl(irajMessagesEl, message, true);
			} else {
				// no irajMessages found, let's append to global messages
				irajMessageService.displayGlobalMessage(message);
			}
		};
		
		irajMessageService.displayGlobalMessage = function(message) {
			var irajGlobalMessagesEl = jQuery("[irajGlobalMessages]");
			if (irajGlobalMessagesEl.length == 0) {
				// the global messages element doesn't exist, let's create a modal one
				jQuery(document.body).append('<div irajGlobalMessages class="modal hide fade"><div class="modal-body"/><div class="modal-footer"><button class="btn btn-primary" data-dismiss="modal">OK</button></div></div>');
				irajGlobalMessagesEl = jQuery("[irajGlobalMessages]");
				irajGlobalMessagesEl.find('button').on('click', irajMessageService.clearGlobalMessages);
			}
			if (irajGlobalMessagesEl.hasClass("modal")) {
				// this is a modal, let's append the message to its body
				irajMessageService.appendMessageAlertToEl(irajGlobalMessagesEl.find('.modal-body'), message, false);
				// and show it
				irajGlobalMessagesEl.modal('show');
			} else {
				// this is not a modal, let's just append the message to it
				irajMessageService.appendMessageAlertToEl(irajGlobalMessagesEl, message, true);
			}
		};
		
		irajMessageService.appendMessageAlertToEl = function(element, message, closeButton) {
			element.append('<div class="alert alert-'+message.type+'">'+(closeButton ? '<button type="button" class="close" data-dismiss="alert">&times;</button>' : '')+message.message+'</div>');
		};
		
		irajMessageService.clearMessages = function(irajMessagesValue) {
			var irajMessagesEl = jQuery("[irajMessages^='"+irajMessagesValue+"']");
			if (irajMessagesEl.length > 0) {
				irajMessagesEl.empty();
			}
		};
		
		irajMessageService.clearGlobalMessages = function() {
			var irajGlobalMessagesEl = jQuery("[irajGlobalMessages]");
			if (irajGlobalMessagesEl.length > 0) {
				if (irajGlobalMessagesEl.hasClass("modal")) {
					// It's a modal, clear only .modal-body div
					irajGlobalMessagesEl.find('.modal-body').empty();
				} else {
					irajGlobalMessagesEl.empty();
				}
			}
		};
		
		irajMessageService.clearAllMessages = function(idPrefix) {
			irajMessageService.clearFieldMessages(idPrefix);
			irajMessageService.clearMessages(idPrefix);
			irajMessageService.clearGlobalMessages();
		};
		
		return irajMessageService;
	})
;
