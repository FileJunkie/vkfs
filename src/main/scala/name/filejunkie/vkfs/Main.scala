package name.filejunkie.vkfs

import java.nio.file.Paths

import name.filejunkie.vkfs.fuse.FS
import name.filejunkie.vkfs.vk.VkApi

object Main {
  def main(args: Array[String]) = {
    args(0) match {
      case "authorize" => {
        val vkApi = new VkApi(args(1), None)
        vkApi.authorize
      }
      case _ => {
        val mountDir = args(0)
        val userId = args(1)
        val token = args.size match {
          case s if s > 2 => Some(args(2))
          case _ => None
        }

        val fs = new FS(userId, token)
        try{
          fs.mount(Paths.get(mountDir), true)
        }
        finally{
          fs.umount()
        }
      }
    }
  }  
}