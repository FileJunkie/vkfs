package name.filejunkie.vkfs.fuse

import com.sun.security.auth.module.UnixSystem

import jnr.ffi.Pointer
import jnr.ffi.types.off_t
import jnr.ffi.types.size_t
import name.filejunkie.vkfs.common.images.Photo
import name.filejunkie.vkfs.vk.VkApi
import ru.serce.jnrfuse.ErrorCodes
import ru.serce.jnrfuse.FuseFillDir
import ru.serce.jnrfuse.FuseStubFS
import ru.serce.jnrfuse.struct.FileStat
import ru.serce.jnrfuse.struct.FuseFileInfo

class FS(userId: String, token: Option[String]) extends FuseStubFS {
  val vkApi = new VkApi(userId, token)
  val unixSystem = new UnixSystem
  val uid = unixSystem.getUid
  val gid = unixSystem.getGid
  
  override def getattr(path: String, stat: FileStat) : Int = {
    def setCorrectDirStat() = {
      val permissions = token match {
        case Some(s) => FileStat.ALL_READ | FileStat.S_IWUSR
        case _ => FileStat.ALL_READ
      }

      stat.st_mode.set(FileStat.S_IFDIR | permissions | FileStat.S_IXUSR | FileStat.S_IXGRP | FileStat.S_IXOTH )
      stat.st_nlink.set(2)
    }

    path match {
      case p if p.endsWith(".jpg") =>{
        stat.st_mode.set(FileStat.S_IFREG | FileStat.ALL_READ)
        stat.st_nlink.set(1)

        val photoId = p.substring(p.lastIndexOf("/") + 1, p.length() - 4)

        val size = vkApi.getPhotoSize(photoId)
        stat.st_size.set(size)
      }
      case "/" => {
        setCorrectDirStat()
      }
      case _ => {
        val realDirname = path.substring(1)

        if(!vkApi.getAlbums.exists { album => album.title == realDirname }){
          return -ErrorCodes.ENOENT
        }
        setCorrectDirStat()
      }
    }

    stat.st_atim.tv_sec.set(System.currentTimeMillis() / 1000)
    stat.st_ctim.tv_sec.set(System.currentTimeMillis() / 1000)
    stat.st_mtim.tv_sec.set(System.currentTimeMillis() / 1000)
    stat.st_uid.set(uid)
    stat.st_gid.set(gid)

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
          case _ => -ErrorCodes.ENOENT
        }
      }
    }
  }

  override def open(path: String, fi: FuseFileInfo) = {
    path match {
      case p if (p.contains("/") && p.endsWith(".jpg")) => 0
      case _ => -ErrorCodes.ENOENT
    }
  }

  override def read(path: String, buf: Pointer, @size_t size: Long, @off_t offset: Long, fi: FuseFileInfo) : Int = {
    path match {
      case p if (p.contains("/") && p.endsWith(".jpg")) => {
        val photoId = p.substring(p.lastIndexOf("/") + 1, p.length() - 4)

        val bytes = vkApi.getPhoto(photoId)

        val length = bytes.length

        offset match {
          case o if o < length => {
            val s = Math.min(bytes.length - o, size).intValue()

            buf.put(0, bytes, offset.intValue(), s)

            s
          }
          case _ => 0
        }
      }
      case _ => -ErrorCodes.ENOENT
    }
  }

  override def rename(oldPath: String, newPath: String): Int = {
    oldPath match {
      case op if (op.count(_ == '/') == 1 && !op.endsWith(".jpg")) => {
        val cleanOldPath = oldPath.substring(1)
        val cleanNewPath = newPath.substring(1,newPath.lastIndexOf('/'))

        val res = vkApi.renameAlbum(cleanOldPath, cleanNewPath)

        res match {
          case true => 0
          case false => -1
        }
      }
      case _ => -1
    }
  }

  override def mkdir(name: String, mode: Long): Int = {
    if(vkApi.createAlbum(name.substring(1))){
      0
    }
    else{
      -ErrorCodes.EACCES
    }
  }
}
