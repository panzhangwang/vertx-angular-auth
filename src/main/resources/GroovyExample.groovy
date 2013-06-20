import com.jetdrone.vertx.yoke.middleware.*
import com.jetdrone.vertx.yoke.GYoke
import com.jetdrone.vertx.yoke.engine.GroovyTemplateEngine
import com.jetdrone.vertx.yoke.util.Utils

import javax.crypto.Mac
import static groovy.json.JsonOutput.toJson
import static java.util.UUID.randomUUID

def secret = Utils.newHmacSHA256("secret here")
def storage = new HashMap<String, String>()

def router = new GRouter()
    .get("/") {request ->
		request.response.sendFile "index.html"
	}
	.post("/apps/protect") {req -> 
		req.response.end toJson("Hello world!")
	}
	.post("/auth/login") {request ->
		def body = request.formAttributes
		if (body['username'] == 'user' && body['password'] == 'pass') {
			def sid = randomUUID() as String
			request.setSessionId(sid)
			storage.put(sid, body['username'])
			request.response.end toJson(true)
		} 
		request.response.end toJson(false)
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

new GYoke(vertx)
  .engine('html', new GroovyTemplateEngine())
  .use(new ErrorHandler(true))
  .use(new CookieParser(secret))
  .use(new Session(secret))
  .use(new Logger(container.logger))
  .use(new BodyParser())
  .use("/static", new Static(".")) 
  .use("/apps", secHandler)
  .use(router)
  .listen(8080)