@(username: String)(implicit r: RequestHeader)

$(function() {

    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
    var chatSocket = new WS("@routes.Application.chat(username).webSocketURL()")
    $("#onChat").show()
    
    var sendMessage = function() {
        chatSocket.send(JSON.stringify( {text: $("#talk").val()} ))
        $("#talk").val('')
    }

    var receiveEvent = function(event) {
        var data = JSON.parse(event.data)

        // Handle errors
        if(data.error) {
            chatSocket.close()
            $("#onError span").text(data.error)
            $("#onError").show()
            return
        } else $("#onChat").show()
        
        if($.inArray('@username', data.targetSet) < 0) return

        // Create the message element
        var el = $('<div class="message"><span></span><p></p></div>')
        $("span", el).text(data.user)
        $("p", el).text(data.message)
        $(el).addClass(data.kind)
        if(data.user == '@username') $(el).addClass('me')
        if(data.user == "Robot") $(el).addClass('robot')
        if(data.evils != null) {
        	$('#evils').html('')
        	$(data.evils).each(function() {
        		var li = document.createElement('li');
        		li.textContent = this;
        		$('#evils').append(li);
        	})
        }
        if(data.roll != null) {
        	$('#roll').html('')
        	var li = document.createElement('li')
        	li.textContent = data.roll
        	$('#roll').append(li)
        }
        if(data.merlin != null) {
        	$('#merlin').html('')
        	var li = document.createElement('li')
        	li.textContent = data.merlin
        	$('#merlin').append(li)
        }
        if(data.lady != null) {
        	$('#lady').html('')
        	var li = document.createElement('li')
        	li.textContent = data.lady
        	$('#lady').append(li)
        }
        if(data.ladied != null) {
        	$('#ladied').html('')
        	$(data.ladied).each(function() {
        		var li = document.createElement('li');
        		li.textContent = this;
        		$('#ladied').append(li);
        	})
        }
        if(data.leader != null) {
        	$('#leader').html('')
        	var li = document.createElement('li')
        	li.textContent = data.leader
        	$('#leader').append(li)
        }
        $('#messages').prepend(el)
        
        $('#main').height($('#messages').height() + $('talk').height() + 120)

        // Update the members list
        $("#members").html('')
        $(data.members).each(function() {
            var li = document.createElement('li');
            li.textContent = this;
            $("#members").append(li);
        })
    }

    var handleReturnKey = function(e) {
        if(e.charCode == 13 || e.keyCode == 13) {
            e.preventDefault()
            sendMessage()
        }
    }

    $("#talk").keypress(handleReturnKey)

    chatSocket.onmessage = receiveEvent
})