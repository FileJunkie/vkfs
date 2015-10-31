package name.filejunkie.vkfs

import scalaj.http._

object Main {
  def main(args: Array[String]) = {
    val userId = args(0)
    val response: HttpResponse[String] = Http("http://api.vk.com/method/photos.getAlbums").param("owner_id",userId).asString
    println(s"Response returned with code ${response.code}, headers ${response.headers}, body ${response.body}") 
  }  
}