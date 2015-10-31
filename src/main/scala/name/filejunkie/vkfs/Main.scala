package name.filejunkie.vkfs

import name.filejunkie.vkfs.vk.VkApi

object Main {
  def main(args: Array[String]) = {
    val userId = args(0)

    val vkApi = new VkApi(userId)

    println(vkApi.getAlbums)
  }  
}