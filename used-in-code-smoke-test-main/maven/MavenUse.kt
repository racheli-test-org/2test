package maven.use

import org.springframework.web.context.ServletContextAware
import org.apache.logging.log4j.core.impl.Log4jContextFactory
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.SigningKeyResolverAdapter

class MavenUse{
    val greeting: String
        get() {
            return "Hello World!"
        }
}

fun main() {
    println(App().greeting)
}