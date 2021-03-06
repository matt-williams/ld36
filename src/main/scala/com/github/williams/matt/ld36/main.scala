package com.github.williams.matt.ld36

import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.scalajs.js.JSApp
import scala.math.{ Pi, min, max, rint }
import scala.util.Random
import org.scalajs.dom
import org.scalajs.dom.raw.{ HTMLElement, HTMLImageElement }
import org.denigma.threejs._
import org.denigma.threejs.extensions.SceneContainer
import org.denigma.threejs.extensions.controls.{ HoverControls, CameraControls }

@js.native
@JSName("THREE.PlaneBufferGeometry")
class PlaneBufferGeometry extends BufferGeometry {
  def this(width: Double, height: Double) = this()
}

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
  def isRunning(): Boolean = js.native
  def setLoop(mode: Int, repetitions: Int): AnimationMixerAction = js.native
}

@js.native
@JSName("THREE")
object MyTHREE extends js.Object {
  var LoopOnce: Int = js.native
  var LoopRepeat: Int = js.native
  var LoopPingPong: Int = js.native
}

@js.native
@JSName("THREE.JSONLoader")
class JSONLoader extends Loader {
  def this(manager: LoadingManager = js.native) = this()
  var withCredentials: Boolean = js.native
  def load(url: String, callback: js.Function2[JSonLoaderResultGeometry, js.Array[Material], Unit], texturePath: String = js.native): Unit = js.native
  def loadAjaxJSON(context: JSONLoader, url: String, callback: js.Function2[Geometry, js.Array[Material], Unit], texturePath: String = js.native, callbackProgress: js.Function1[Progress, Unit] = js.native): Unit = js.native
  def parse(json: String, texturePath: String): js.Dynamic = js.native
}

object Main extends JSApp {
  def main(): Unit = {
    val el: HTMLElement = dom.document.getElementById("container").asInstanceOf[HTMLElement]
    val scene = new Scene(el, dom.window.innerWidth, dom.window.innerHeight)
    scene.render()
  }
}

trait Container3D extends SceneContainer {
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

  container.appendChild(renderer.domElement)

  override def onEnterFrame(): Unit = {
    renderer.render(scene, camera)
  }
}

class Golem(geometry: Geometry, material: MeshBasicMaterial, mixer: AnimationMixer) {
  private val mesh = new SkinnedMesh(geometry, material)
  mesh.position.set(-0.2, -0.65, 0);

  val camera = new PerspectiveCamera(70, 1.0, 0.1, 1000);
  camera.position.set(0.8, 1.45, 0);
  camera.rotateY(-Math.PI / 2);
 
  val bufferTexture = new WebGLRenderTarget(512, 512, js.Dynamic.literal(
    minFilter = 1006, // THREE.LinearFilter,
    magFilter = 1003 // THREE.NearestFilter
  ).asInstanceOf[WebGLRenderTargetOptions]);

  val bufferMaterial = new MeshBasicMaterial(js.Dynamic.literal(
    map = bufferTexture
  ).asInstanceOf[MeshBasicMaterialParameters]);

  val object3d = new Object3D()
  object3d.add(mesh)
  object3d.add(camera)

  val position = object3d.position;
  var _orientation: Double = 0;
  val up: Vector3 = new Vector3(0, 1, 0)
  val forward: Vector3 = new Vector3(1, 0, 0)
  def orientation = _orientation;
  def orientation_= (value: Double):Unit = {
    object3d.rotateY(value - _orientation)
    forward.applyAxisAngle(up, value - _orientation);
    _orientation = value;
  }

  private val walkBegin = mixer.clipAction("walk.begin", mesh)
  private val walkCycle = mixer.clipAction("walk.cycle", mesh)
  private val walkEnd = mixer.clipAction("walk.end", mesh)

  private val target = new Vector3()
  def walkTo(x: Double, z: Double): Unit = {
    target.set(x, 0, z)
    if ((position.distanceTo(target) > 1) &&
        (!walkBegin.isRunning()) &&
        (!walkCycle.isRunning())) {
      walkEnd.stop();
      walkBegin.setLoop(MyTHREE.LoopOnce, 1).play();
      walkCycle.startAt(0.833333).play();
    }
  }

  def isWalking(): Boolean = walkBegin.isRunning() || walkCycle.isRunning() || walkEnd.isRunning()

  private var time: Double = 0
  private var lastMovedTime: Double = 0
  def timeSinceLastMoved(): Double = {
    return time - lastMovedTime;
  }

  def update(delta: Double, golems: List[Golem]): Unit = {
    time = time + delta
    if (isWalking()) {
      val bearingVector = target.clone().sub(position).normalize()
      val bearing = forward.angleTo(bearingVector)
      val bearingDelta = min(Pi / 32, bearing)
      val trialForward = forward.clone().applyAxisAngle(up, bearingDelta)
      if (trialForward.angleTo(bearingVector) > bearing) {
        orientation -= bearingDelta
      } else {
        orientation += bearingDelta
      }

      val currentX = rint((position.x + bearingVector.x) / 4).toInt
      val currentZ = rint((position.z + bearingVector.z) / 4).toInt
      var nextX = currentX + rint(bearingVector.x).toInt
      var nextZ = currentZ + rint(bearingVector.z).toInt

      def filterFunction(otherGolem: Golem): Boolean = {
        if (this == otherGolem) { return false; }
        val otherX = rint(otherGolem.position.x / 4).toInt;
        val otherZ = rint(otherGolem.position.z / 4).toInt;
        if ((currentX == otherX) && (currentZ == otherZ)) { return true; }
        if ((nextX == otherX) && (nextZ == otherZ)) { return true; }
        return false;
      }

      val obstructingGolems = golems.filter(filterFunction)

      if (obstructingGolems.length > 0) {
        if ((walkBegin.isRunning()) ||
            (walkCycle.isRunning())) {
          walkBegin.stop();
          walkCycle.stop();
          walkEnd.setLoop(MyTHREE.LoopOnce, 1).play();
        }
      } else {
        if ((position.distanceTo(target) < 1.2 * 1.66666) &&
            ((walkBegin.isRunning()) ||
             (walkCycle.isRunning()))) {
          walkBegin.stop();
          walkCycle.stop();
          walkEnd.play();
        }
        // object3d automatically considers forward vector when translating
        val oldX = rint(position.x / 4).toInt
        val oldZ = rint(position.z / 4).toInt
        if (position.distanceTo(target) < 1.2 * delta) {
          walkEnd.stop();
          object3d.translateX(position.distanceTo(target));
        } else {
          object3d.translateX(1.2 * delta);
        }
        if ((rint(position.x / 4).toInt != oldX) ||
            (rint(position.z / 4).toInt != oldZ)) {
println(oldX + ", " + oldZ + " => " + rint(position.x / 4).toInt + ", " + rint(position.z / 4).toInt);
          lastMovedTime = time;
        }
      }
    }
  }

  def think(accessible: Array[Array[Boolean]]) {
    if (!isWalking() && (Random.nextInt(50) == 0)) {
      val currentX = rint(position.x / 4).toInt
      val currentZ = rint(position.z / 4).toInt
      val (deltaX: Int, deltaZ: Int) = Random.nextInt(4) match {
        case 0 => (0, 1)
        case 1 => (1, 0)
        case 2 => (0, -1)
        case 3 => (-1, 0)
      }
      var freeRange = 1;
//println("thinking: " + currentX + " + " + deltaX + ", " + currentZ + ", " + deltaZ);
      while (accessible(currentX + deltaX * freeRange)(currentZ + deltaZ * freeRange)) {
        freeRange = freeRange + 1;
//println("thinking: " + currentX + " + " + deltaX + " * " + freeRange + ", " + currentZ + " + " + deltaZ + " * " + freeRange);
      }
      val range = Random.nextInt(freeRange)
//println("thinking: " + range)
      walkTo((currentX + deltaX * range) * 4, (currentZ + deltaZ * range) * 4)
    }
  }

  def renderFromViewpoint(renderer: WebGLRenderer, scene: org.denigma.threejs.Scene) {
    renderer.render(scene, camera, bufferTexture);
  }
}

class Tile() {
  private var _geometry: Option[Geometry] = None;
  private var _texture: Option[Texture] = None;
  private var _material: Option[Material] = None;

  def geometry = _geometry.getOrElse(null)
  def geometry_= (geometry: Geometry) = {
    _geometry = Option(geometry);
  }
  def texture = _texture.getOrElse(null)
  def texture_= (texture: Texture) = {
    _texture = Option(texture);
    _material = Option(new MeshBasicMaterial(js.Dynamic.literal(map = texture).asInstanceOf[MeshBasicMaterialParameters]));
  }

  def getRotated(orientation: Double): RotatedTile = { return new RotatedTile(this, orientation) }

  def instantiate(scene: org.denigma.threejs.Scene, x: Double, z: Double, orientation: Double) = {
    _geometry match {
      case Some(geometry) => _material match {
        case Some(material) => {
       	  val mesh = new Mesh(geometry, material);
          mesh.rotateY(orientation * Pi / 2);
          mesh.position.set(x * 4, 0, z * 4);
          scene.add(mesh);
        }
        case None => ()
      }
      case None => ()
    }
  }
}

class RotatedTile(val tile: Tile, val orientation: Double) {
  def instantiate(scene: org.denigma.threejs.Scene, x: Double, z: Double) = {
    tile.instantiate(scene, x, z, orientation)
  }
}

class Scene(val container: HTMLElement, val width: Double, val height: Double) extends Container3D {
  private var mouseX:Double = 0
  private var mouseY:Double = 0
  private var mouseDown: Boolean = false;
  container.onmousemove = {
    (e: dom.MouseEvent) => {
      mouseX = (e.clientX / width) * 2 - 1;
      mouseY = (e.clientY / height) * 2 - 1;
    }
  }
  container.onmousedown = {
    (e: dom.MouseEvent) => {
      mouseDown = true;
    }
  }
  container.onmouseup = {
    (e: dom.MouseEvent) => {
      mouseDown = false;
    }
  }

  val innerScene = new org.denigma.threejs.Scene();

  val raycaster = new Raycaster();
  val mixer = new AnimationMixer(innerScene)

  val emptyTile = new Tile();
  val wallTile = new Tile();
  val internalCornerTile = new Tile();
  val externalCornerTile = new Tile();
  val nullTile = new Tile();

  var golemGeometry: Geometry = null;
  var golemGoodMaterial: MeshBasicMaterial = null;
  var golemEvilMaterial: MeshBasicMaterial = null;

  var goodGolems: List[Golem] = List()
  var evilGolems: List[Golem] = List()
  var mapAccessible: Array[Array[Boolean]] = null;

  var manager = new LoadingManager(
    () => {
      val xhr = new dom.XMLHttpRequest()
      xhr.open("GET", "map.txt")
      xhr.onload = { (e: dom.Event) =>
        if (xhr.status == 200) {
          var z = 0;
          var maxX = 0;
          for (line <- xhr.responseText.split("\n")) {
            for (x <- 0 to line.length / 2 - 1) {
              val tileChar = line.charAt(x * 2)
              val tile = tileChar match {
                case ' ' => emptyTile
                case 'g' => emptyTile
                case 'e' => emptyTile
                case '-' => wallTile
                case '+' => internalCornerTile
                case 'L' => externalCornerTile
                case _ => nullTile
              }
              val orientation = line.charAt(x * 2 + 1) match {
                case '>' => 0
                case '^' => 1
                case '<' => 2
                case 'v' => 3
                case _ => 0
              }
              if ((tileChar == 'g') || (tileChar == 'e')) {
                val golem = new Golem(golemGeometry, if (tileChar == 'g') golemGoodMaterial else golemEvilMaterial, mixer);
                innerScene.add(golem.object3d);
                golem.position.set(x * 4, 0, z * 4);
                golem.orientation = orientation * Pi / 2;
                //golem.walkTo(0, -4)
                if (tileChar == 'g') {
                  goodGolems = golem :: goodGolems;
                } else {
                  evilGolems = golem :: evilGolems;
                }
              }
              tile.instantiate(innerScene, x, z, orientation)
            }
            z = z + 1;
            maxX = max(maxX, line.length / 2 - 1);
          }

          mapAccessible = Array.fill[Boolean](maxX + 1, z)(false);
          z = 0
          for (line <- xhr.responseText.split("\n")) {
            for (x <- 0 to line.length / 2 - 1) {
              val tileChar = line.charAt(x * 2)
              if ((tileChar == ' ') || (tileChar == 'g') || (tileChar == 'e')) {
                mapAccessible(x)(z) = true;
              }
            }
            z = z + 1;
          }

          var x = 0
          for (golem <- Random.shuffle(goodGolems)) {
            val plane = new PlaneBufferGeometry(3000, 3000);
            val planeObject = new Mesh(plane, golem.bufferMaterial);
            planeObject.position.x = (x - goodGolems.length / 2) * 3400;
            planeObject.position.z = -4000
            scene.add(planeObject);
            x = x + 1;
          }
        }
      }
      xhr.send()
    },
    (string: String, double1: Double, double2: Double) => {},
    () => {}
  );

  val textureLoader = new TextureLoader(manager);
  val jsonLoader = new JSONLoader(manager);

  jsonLoader.load("floor-ceiling.json", (geometry: JSonLoaderResultGeometry, materials: js.Array[Material]) => {
    emptyTile.geometry = geometry;
  });
  textureLoader.load("floor-ceiling.png", (texture: Texture) => {
    emptyTile.texture = texture;
  });
  jsonLoader.load("wall.json", (geometry: JSonLoaderResultGeometry, materials: js.Array[Material]) => {
    wallTile.geometry = geometry;
  });
  textureLoader.load("wall.png", (texture: Texture) => {
    wallTile.texture = texture;
  });
  jsonLoader.load("corner-internal.json", (geometry: JSonLoaderResultGeometry, materials: js.Array[Material]) => {
    internalCornerTile.geometry = geometry;
  });
  textureLoader.load("corner-internal.png", (texture: Texture) => {
    internalCornerTile.texture = texture;
  });
  jsonLoader.load("corner-external.json", (geometry: JSonLoaderResultGeometry, materials: js.Array[Material]) => {
    externalCornerTile.geometry = geometry;
  });
  textureLoader.load("corner-external.png", (texture: Texture) => {
    externalCornerTile.texture = texture;
  });

  jsonLoader.load("golem.json", (geometry: JSonLoaderResultGeometry, materials: js.Array[Material]) => {
    golemGeometry = geometry;
    val anyGeometry = js.Dynamic.literal(geometry = geometry).geometry;
    for (i <- 0 to 2) {
      mixer.clipAction(anyGeometry.animations.selectDynamic(i.toString), ());
    }
  });
  textureLoader.load("golem-good.png", (texture: Texture) => {
    golemGoodMaterial = new MeshBasicMaterial(js.Dynamic.literal(
      map = texture,
      skinning = true
    ).asInstanceOf[MeshBasicMaterialParameters]);
  });
  textureLoader.load("golem-evil.png", (texture: Texture) => {
    golemEvilMaterial = new MeshBasicMaterial(js.Dynamic.literal(
      map = texture,
      skinning = true
    ).asInstanceOf[MeshBasicMaterialParameters]);
  });



  var gameOver = false;
  override def onEnterFrame(): Unit = {
    if (mouseX > 0.9) {
      camera.translateX(30);
      camera.rotateY(-0.01)
    } else if (mouseX < -0.9) {
      camera.translateX(-30);
      camera.rotateY(0.01)
    }

    if (mouseDown) {
      raycaster.setFromCamera(new Vector2(mouseX, mouseY), camera)
      for (intersect <- raycaster.intersectObjects(scene.children)) {
        for (golem <- goodGolems) {
          if (intersect.`object`.asInstanceOf[Mesh].material == golem.bufferMaterial) {
            val forwardX = rint(golem.forward.x).toInt
            val forwardZ = rint(golem.forward.z).toInt
            var newX = rint(golem.position.x / 4).toInt
            var newZ = rint(golem.position.z / 4).toInt
            if (intersect.point.y < -1000) {
              newX = newX + forwardX
              newZ = newZ + forwardZ
            } else if (intersect.point.y > 1000) {
              newX = newX - forwardX
              newZ = newZ - forwardZ
            } else if (intersect.point.x - intersect.`object`.position.x < -1000) {
              newX = newX + forwardZ
              newZ = newZ - forwardX
            } else if (intersect.point.x - intersect.`object`.position.x > 1000) {
              newX = newX - forwardZ
              newZ = newZ + forwardX
            }
            if (mapAccessible(newX)(newZ)) {
              golem.walkTo(newX * 4, newZ * 4)
            }
          }
        }
      }
    }

    mixer.update(0.03)
    val golems = goodGolems ::: evilGolems
    for (golem <- golems) {
      golem.update(0.03, golems)
    }
    for (golem <- evilGolems) {
      golem.think(mapAccessible)
    }
    for (golem <- goodGolems) {
      golem.renderFromViewpoint(renderer, innerScene);
    }
    val blockedGolems = evilGolems.filter((golem: Golem) => golem.timeSinceLastMoved() > 20)
    if ((evilGolems.length > 0) &&
        (blockedGolems.length == evilGolems.length) &&
        (!gameOver)) {
      dom.window.alert("All evil golems blocked - you win!");
      gameOver = true;
    }

    super.onEnterFrame()
  }
}
