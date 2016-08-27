package com.github.williams.matt.ld36

import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.scalajs.js.JSApp
import scala.math.Pi
import scala.util.Random
import org.scalajs.dom
import org.scalajs.dom.raw.{ HTMLElement, HTMLImageElement }
import org.denigma.threejs._
import org.denigma.threejs.extensions.SceneContainer
import org.denigma.threejs.extensions.controls.{ HoverControls, CameraControls }

@js.native
@JSName("THREE.AnimationMixer")
class AnimationMixer extends js.Object {
  def this(root: org.denigma.threejs.Scene = js.native) = this()
  def clipAction(clip: js.Any, root: js.Any): AnimationMixerAction = js.native
  def update(delta: Double): AnimationMixer = js.native
}

@js.native
@JSName("THREE.AnimationMixer._Action")
class AnimationMixerAction extends js.Object {
  def play(): AnimationMixerAction = js.native
  def stop(): AnimationMixerAction = js.native
  def reset(): AnimationMixerAction = js.native
  def startAt(time: Double): AnimationMixerAction = js.native
  def setLoop(mode: Int, repetitions: Int): AnimationMixerAction = js.native
}

@js.native
@JSName("THREE")
object MyTHREE extends js.Object {
  var LoopOnce: Int = js.native
  var LoopRepeat: Int = js.native
  var LoopPingPong: Int = js.native
}

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
  val mixer = new AnimationMixer(scene)

  container.appendChild(renderer.domElement)

  override def onEnterFrame(): Unit = {
    controls.update()
    mixer.update(0.015)
    renderer.render(scene, camera)
  }
}

class Scene(val container: HTMLElement, val width: Double, val height: Double) extends Container3D {
  val textureLoader = new TextureLoader();
  val jsonLoader = new JSONLoader();
  jsonLoader.load("tunnel.json", (geometry: JSonLoaderResultGeometry, materials: js.Array[Material]) => {
    textureLoader.load("tunnel.png", (texture: Texture) => {
      val material = new MeshBasicMaterial(js.Dynamic.literal(
          map = texture
        ).asInstanceOf[MeshBasicMaterialParameters]);
      for (z <- -9 to 9) {
        val mesh = new Mesh(geometry, material);
        mesh.position.set(0, 0, z * -2000);
        mesh.scale.set(500, 500, 500);
        scene.add(mesh);
      }
    });
    ()
  });

  jsonLoader.load("golem.json", (geometry: JSonLoaderResultGeometry, materials: js.Array[Material]) => {
    textureLoader.load("golem.png", (texture: Texture) => {
      val material = new MeshBasicMaterial(js.Dynamic.literal(
          map = texture,
          skinning = true
        ).asInstanceOf[MeshBasicMaterialParameters]);
      val anyGeometry = js.Dynamic.literal(geometry = geometry).geometry;
      for (i <- 0 to 2) {
        mixer.clipAction(anyGeometry.animations.selectDynamic(i.toString), ());
      }
      for (z <- -9 to 9) {
        val mesh = new SkinnedMesh(geometry, material);
        mesh.position.set(-100, -400, z * -2000);
        mesh.scale.set(500, 500, 500);
        mesh.rotateY(Pi / 2);
        scene.add(mesh);

        mixer.clipAction("walk.begin", mesh).setLoop(MyTHREE.LoopOnce, 1).play();
        mixer.clipAction("walk.cycle", mesh).startAt(1.0).play();
        //mixer.clipAction("walk.end", mesh).play();
      }
    });
    ()
  });

  val light = new DirectionalLight(0xffffff, 2)
  light.position.set(1, 1, 1).normalize()
  scene.add(light)
}
