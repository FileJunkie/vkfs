package name.filejunkie.vkfs.fuse

import jnr.ffi.Pointer
import jnr.ffi.types.off_t
import name.filejunkie.vkfs.vk.VkApi
import ru.serce.jnrfuse.FuseFillDir
import ru.serce.jnrfuse.FuseStubFS
import ru.serce.jnrfuse.struct.FileStat
import ru.serce.jnrfuse.struct.FuseFileInfo
import ru.serce.jnrfuse.ErrorCodes
import name.filejunkie.vkfs.common.images.Photo

class FS(userId: String) extends FuseStubFS {
  val vkApi = new VkApi(userId)
  
  override def getattr(path: String, stat: FileStat) : Int = {
    path match {
      case c if c.endsWith(".jpg") =>{
        stat.st_mode.set(FileStat.S_IFREG | FileStat.S_IRUSR | FileStat.S_IRGRP | FileStat.S_IROTH )
        stat.st_nlink.set(1)
      }
      case _ => {
        stat.st_mode.set(FileStat.S_IFDIR | FileStat.S_IRUSR | FileStat.S_IXUSR | FileStat.S_IRGRP | FileStat.S_IXGRP | FileStat.S_IROTH | FileStat.S_IXOTH )
        stat.st_nlink.set(2)
      }
    }
    0
  }
  
  override def readdir(path: String, buf: Pointer, filter: FuseFillDir, @off_t offset: Long, fi: FuseFileInfo) : Int = {
    path match {
      case "/" => {
        filter.apply(buf, ".", null, 0)
        filter.apply(buf, "..", null, 0)

        vkApi.getAlbums.foreach { album => filter.apply(buf, album.title, null, 0) }

        0
      }
      case _ => {
        vkApi.getPhotos(path.substring(1)) match {
          case Some(photos: List[Photo]) => {
            filter.apply(buf, ".", null, 0)
            filter.apply(buf, "..", null, 0)

            photos.foreach { photo => filter.apply(buf, photo.id.toString() + ".jpg", null, 0) }

            0
          }
          case _ => ErrorCodes.ENOENT()
        }
      }
    }
  }
}