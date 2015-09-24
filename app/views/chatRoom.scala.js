@(username: String)(implicit r: RequestHeader)

$(function() {

    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
    var chatSocket = null
    $("#onChat").show()
    
    var sendMessage = function(message) {
        chatSocket.send(JSON.stringify( {text: message} ))
    }

    var connectionCloseEvent = function(event) {
        var el = $('<div class="message"><span></span><p></p></div>')
        $("span", el).text('system')
        $("p", el).text('connection lost')
        $(el).addClass('robot')
        $('#messages').prepend(el)
        $('#main').height($('#messages').height() + $('talk').height() + 220)
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

        if(data.message.startsWith('/ping')) {
            sendMessage("/pong")
            return
        }

        if(data.message.startsWith('/pong')) return
        if($.inArray('@username', data.targetSet) < 0) return

        // Create the message element
        var el = $('<div class="message"><span></span><p></p></div>')
        $("span", el).text(data.user)
        $("p", el).text(data.message)
        $(el).addClass(data.kind)
        if(data.user == '@username') $(el).addClass('me')
        if(data.user == "Robot") $(el).addClass('robot')
        if(data.message.length > 0) $('#messages').prepend(el)
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
        if(data.players != null) {
        	$('#players').html('')
        	$('#electList').html('')
            $('#assassinList').html('')
        	$(data.players).each(function() {
        		var li = document.createElement('li');
        		li.textContent = this;
        		$('#players').append(li);
        		var option = document.createElement('option')
        		option.textContent = this;
        		$('#electList').append(option)
                $('#assassinList').append(option)
        	})
        }
        if(data.elected != null) {
        	$('#elected').html('')
        	$(data.elected).each(function() {
        		var li = document.createElement('li');
        		li.textContent = this;
        		$('#elected').append(li);
        	})
        }
        if(data.voted != null) {
        	$('#voted').html('')
        	$(data.voted).each(function() {
        		var li = document.createElement('li');
        		li.textContent = this;
        		$('#voted').append(li);
        	})
        }
        if(data.leaderOrder != null) {
        	$('#leaderOrder').html('')
        	$(data.leaderOrder).each(function() {
        		var li = document.createElement('li');
        		li.textContent = this;
        		if(this == data.leader) li.textContent = "☆ " + this + " ☆";
        		$('#leaderOrder').append(li);
        	})
        }
        
        $('#main').height($('#messages').height() + $('talk').height() + 220)

        // Update the members list
        $("#members").html('')
        $(data.members).each(function() {
            var li = document.createElement('li');
            li.textContent = this;
            $("#members").append(li);
        })
    }

    var sendButtonClicked = function() {
    	sendMessage($("#talk").val())
        $("#talk").val('')
    }
    $('#send').click(sendButtonClicked)
    
    var handleReturnKey = function(e) {
        if(e.charCode == 13 || e.keyCode == 13) {
            e.preventDefault()
            sendButtonClicked()
        }
    }
    $("#talk").keypress(handleReturnKey)
    
    var statusButtonClicked = function() {
    	sendMessage("/status")
    }
    $('#status').click(statusButtonClicked)
    
    var helpButtonClicked = function() {
    	sendMessage("/help")
    }
    $('#help').click(helpButtonClicked)
    
    var voteTrueButtonClicked = function() {
    	sendMessage("/vote true")
    }
    $('#voteTrue').click(voteTrueButtonClicked)
    
    var voteFalseButtonClicked = function() {
    	sendMessage("/vote false")
    }
    $('#voteFalse').click(voteFalseButtonClicked)
    
    var electButtonClicked = function() {
    	sendMessage("/elect " + $('#electList option:selected').text())
    }
    $('#elect').click(electButtonClicked)
    
    var electResetButtonClicked = function() {
    	sendMessage("/elect reset")
    }
    $('#electReset').click(electResetButtonClicked)
    
    var questTrueButtonClicked = function() {
    	sendMessage("/quest true")
    }
    $('#questTrue').click(questTrueButtonClicked)
    
    var questFalseButtonClicked = function() {
    	sendMessage("/quest false")
    }
    $('#questFalse').click(questFalseButtonClicked)

    var assassinButtonClicked = function() {
        sendMessage("/assassin " + $('#electList option:selected').text())
    }
    $('#assassin').click(assassinButtonClicked)

    var ladyButtonClicked = function() {
        sendMessage("/lady " + $('#electList option:selected').text())
    }
    $('#ladyCommand').click(ladyButtonClicked)

    var reconnectButtonClicked = function() {
        if (chatSocket) {
            chatSocket.close(1000, 'user close for reconnect')
            chatSocket.onmessage = null
            chatSocket.onclose = null
        }
        chatSocket = new WS("@routes.Application.chat(username).webSocketURL()")
        chatSocket.onmessage = receiveEvent
        chatSocket.onclose = connectionCloseEvent
    }
    $('#reconnect').click(reconnectButtonClicked)

    reconnectButtonClicked()

    window.onunload = function() {
        if (chatSocket) {
            chatSocket.onclose = null;
        }
    }
})