package name.filejunkie.vkfs

import java.nio.file.Paths

import name.filejunkie.vkfs.fuse.FS
import name.filejunkie.vkfs.vk.VkApi

object Main {
  def main(args: Array[String]) = {
    args(0) match {
      case "authorize" => {
        VkApi.authorize
      }
      case "mount" => {
        val mountDir = args(1)
        val userId = args(2)
        val token = args.size match {
          case s if s > 3 => Some(args(3))
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
      case _ => println("Unknown command")
    }
  }  
}