package org.nanobot

class NanoBot {
    def server
    def port
    def nickname
    def socket
    def ConnectionHandler connection
    def HashMap<String, ArrayList<Closure>> handlers = [:]

    NanoBot(server, port, nickname) {
        this.server = server
        this.port = port
        this.nickname = nickname
    }

    def connect() {
        socket = new Socket()
        socket.connect(new InetSocketAddress(server as String, port as int))
        connection = new ConnectionHandler(this, socket.inputStream.newReader(), new PrintStream(socket.outputStream))
    }

    def dispatch(String name, Object... args) {
        if (handlers.containsKey(name)) {
            ArrayList<Closure> handlers = handlers.get(name)
            for (Closure closure : handlers) {
                closure.curry(args)
                closure.run()
            }
        }
    }

    def on(String name, Closure closure) {
        if (handlers.containsKey(name)) {
            handlers.get(name).add(closure)
        } else {
            handlers.put(name, new ArrayList<Closure>(Arrays.asList(closure)))
        }
    }

    def join(String channel) {
        connection.writer.println('JOIN ' + channel)
    }
}
