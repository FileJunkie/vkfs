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
        val token = args.lift(3)

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