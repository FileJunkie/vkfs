package name.filejunkie.vkfs.vk

import scalaj.http._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import name.filejunkie.vkfs.common.images.Album

class VkApi(userId: String) {
  val apiPrefix = "http://api.vk.com/method/"
  implicit val formats = DefaultFormats
  
  def getAlbums = {
    val response: HttpResponse[String] = Http(apiPrefix + "photos.getAlbums").param("owner_id",userId).param("v","5.37").asString
    val responseString = response.body
    val json = parse(responseString)
    val albumsJson = json \ "response" \ "items"
    
    albumsJson.extract[List[Album]]
  }
}