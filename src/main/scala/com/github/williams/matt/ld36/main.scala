package com.github.williams.matt.ld36

import scala.scalajs.js
import scala.scalajs.js.JSApp
import scala.util.Random
import org.scalajs.dom
import org.scalajs.dom.raw.{ HTMLElement, HTMLImageElement }
import org.denigma.threejs._
import org.denigma.threejs.extensions.SceneContainer
import org.denigma.threejs.extensions.controls.{ HoverControls, CameraControls }

object Main extends JSApp {
  def main(): Unit = {
    val el: HTMLElement = dom.document.getElementById("container").asInstanceOf[HTMLElement]
    val scene = new Scene(el, 1280, 500)
    scene.render()
  }
}

trait Container3D extends SceneContainer {
  container.style.width = width.toString
  container.style.height = height.toString
  container.style.position = "relative"

  override type RendererType = WebGLRenderer

  val absolute = "absolute"
  val positionZero = "0"

  protected def initRenderer() = {
    val params = js.Dynamic.literal(
      antialias = true
      ).asInstanceOf[WebGLRendererParameters]
    val vr = new WebGLRenderer(params)

    vr.domElement.style.position = absolute
    vr.domElement.style.top = positionZero
    vr.domElement.style.margin = positionZero
    vr.domElement.style.padding = positionZero
    vr.setSize(width, height)
    vr
  }

  val controls: CameraControls = new HoverControls(camera, this.container)

  container.appendChild(renderer.domElement)

  override def onEnterFrame(): Unit = {
    controls.update()
    renderer.render(scene, camera)
  }
}

class Scene(val container: HTMLElement, val width: Double, val height: Double) extends Container3D {
  var textureLoader = new TextureLoader();
  val jsonLoader = new JSONLoader();
  jsonLoader.load("tunnel.json", (geometry: JSonLoaderResultGeometry, materials: js.Array[Material]) => {
    textureLoader.load("tunnel.png", (texture: Texture) => {
      val material = new MeshBasicMaterial(js.Dynamic.literal(
          map = texture
        ).asInstanceOf[MeshBasicMaterialParameters]);
      for (x <- 0 to 9) {
        val mesh = new Mesh(geometry, material);
        mesh.position.set(0, 0, x * -2000);
        mesh.scale.set(500, 500, 500);
        scene.add(mesh);
      }
    });
    ()
  });


/*
  val geometry = new BoxGeometry(350, 300, 250)

  val colors = List("green", "red", "blue", "orange", "purple", "teal")
  val colorMap = Map(colors.head -> 0xA1CF64, colors(1) -> 0xD95C5C, colors(2) -> 0x6ECFF5,
    colors(3) -> 0xF05940, colors(4) -> 0x564F8A, colors(5) -> 0x00B5AD)

  def materialParams(name: String): MeshLambertMaterialParameters = js.Dynamic.literal(
    color = new Color(colorMap(name))
    ).asInstanceOf[MeshLambertMaterialParameters]

  def randColorName: String = colors(Random.nextInt(colors.size))

  var meshes = addMesh(new Vector3(0, 0, 0)) :: addMesh(new Vector3(400, 0, 200)) :: addMesh(new Vector3(-400, 0, 200)) :: Nil
*/

  val light = new DirectionalLight(0xffffff, 2)
  light.position.set(1, 1, 1).normalize()
  scene.add(light)

/*
  def addMesh(pos: Vector3 = new Vector3()): Mesh = {
    val material = new MeshLambertMaterial(this.materialParams(randColorName))
    val mesh: Mesh = new Mesh(geometry, material)
    mesh.name = pos.toString
    mesh.position.set(pos.x, pos.y, pos.z)
    mesh
  }

  meshes.foreach(scene.add)
*/
}
