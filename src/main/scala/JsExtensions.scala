package play.api.libs.json

package object extensions {

implicit class JsExtensions(val js: JsValue) extends AnyVal {

  def get(path: JsPath): JsValue = {
    JsZipper(js).findPath(path).root.value
  }

  def set(pathValues: (JsPath, JsValue)*): JsValue = {
    JsZipper(js).createOrUpdate(pathValues).root.value
  }
  
  def delete(path: JsPath): JsValue = {
    JsZipper(js).findPath(path).delete.root.value
  }

  def delete(path1: JsPath, path2: JsPath, others: JsPath*): JsValue = {
    val paths = path1 +: path2 +: others.toList
    JsZipper(js)
      .deletePaths(paths).value
  }

  def findAll(f: JsValue => Boolean): Stream[(JsPath, JsValue)] = {
    JsZipper(js).findAllByValue(f) map { zipper => zipper.pathValue }
  }

  def findAll(f: (JsPath, JsValue) => Boolean): Stream[(JsPath, JsValue)] = {
    JsZipper(js).findAllByPathValue(f) map { zipper => zipper.pathValue }
  }

  def updateAll(filterF: JsValue => Boolean)(mapF: JsValue => JsValue): JsValue = {
    JsZipper(js).filterMapThroughByValue(filterF)(mapF).root.value
  }

  def updateAll(filterF: (JsPath, JsValue) => Boolean)(mapF: (JsPath, JsValue) => JsValue): JsValue = {
    JsZipper(js).filterMapThrough{ zipper => 
      filterF(zipper.path, zipper.value)
    }{ zipper => 
      zipper.updatePathNode( (path, node) => Node.copy(node, mapF(path, node.value)) )
    }.root.value
  }

  /** Monadic features */
  import syntax._

  def setM[M[_]: Monad](pathValues: (JsPath, M[JsValue])*): M[JsValue] = {
    JsZipperM[M](js).createOrUpdate(pathValues).map(_.root.value)
  }

  def updateAllM[M[_]](filterF: JsValue => Boolean)(mapF: JsValue => M[JsValue])(implicit m:Monad[M]): M[JsValue] = {
    JsZipperM[M](js).filterMapThrough{ (zipper:JsZipperM[M]) => 
      filterF(zipper.value)
    }{ (zipper:JsZipperM[M]) => 
      zipper.update(mapF)
    }.map(_.root.value)
  }
}

}