import com.jetdrone.vertx.yoke.middleware.*
import com.jetdrone.vertx.yoke.GYoke
import com.jetdrone.vertx.yoke.engine.GroovyTemplateEngine
import com.jetdrone.vertx.yoke.util.Utils
import org.vertx.java.core.json.JsonObject

import javax.crypto.Mac
import static groovy.json.JsonOutput.toJson
import static java.util.UUID.randomUUID

def secret = Utils.newHmacSHA256("secret here")
def storage = vertx.sharedData.getMap('session.store')

def router = new GRouter()
    .get("/") {request ->
		request.response.sendFile "index.html"
	}
	.post("/apps/protect") {request -> 
		def body = request.formAttributes
		request.response.end toJson(body['content'])
	}
	.post("/auth/login") {request ->
		def body = request.formAttributes
		if (body['username'] == 'user' && body['password'] == 'pass') {
			def sid = randomUUID() as String
			request.setSessionId(sid)
			storage.put(sid, body['username'])
			request.response.end toJson(true)
			return
		} 
		request.response.end toJson(false)
	}
	.get("/auth/user") {request, next ->
	    def uname = storage.get(request.sessionId == null ? "" : request.sessionId)
	    def json = new JsonObject().putString("username", uname).putString("sessionId", request.sessionId)
	    request.response.end json
	}
	.post("/auth/logout") {request ->
		def sid = request.getSessionId()
        storage.remove(sid == null ? "" : sid)
		request.setSessionId(null)
		request.response.end toJson("")
	}

def secHandler = { request, next ->
  def sid = request.sessionId
  def uname = storage.get(sid == null ? "" : sid);
  if (!uname) {
	next.handle(401)
	return
  } 
  next.handle(null)	
}

def server = vertx.createHttpServer()
def eb = vertx.eventBus

new GYoke(vertx)
  .engine('html', new GroovyTemplateEngine())
  .use(new ErrorHandler(true))
  .use(new CookieParser(secret))
  .use(new Session(secret))
  .use(new Logger(container.logger))
  .use(new BridgeSecureHandler("auth_address", "session.store" ))
  .use(new BodyParser())
  .use("/static", new Static(".")) 
  .use("/apps", secHandler)
  .use(router)
  .listen(server)

def inboundPermitted = [
  [
      address : 'some-address',
      requires_auth : true
  ]
]
vertx.createSockJSServer(server).bridge(prefix: '/eventbus', inboundPermitted, [[:]], 5 * 60 * 1000, 'auth_address')
server.listen(8080)