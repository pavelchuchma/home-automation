class SimpleWebSocket {
    // Create web socket
    constructor(url) {
        this.url = url;
        this.connect();
    }

    // Connect - not to be called by users
    connect() {
        var self = this;
        this.ws = new WebSocket(this.url);
        this.ws.onmessage = function(event) {
            self.onmessage(JSON.parse(event.data));
        }
        this.ws.onclose = function(event) {
            // Wait and reconnect
            setTimeout(function() { self.connect(); }, 1000);
        };
    }

    // Override this method to handle received json message
    onmessage(json) {}

    // Send message to server
    send(message) {
        this.ws.send(message);
    }    
}
