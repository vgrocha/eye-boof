(ns eye-boof.helpers
  (:require 
    [eye-boof.core :as c]
    [seesaw.core :as w]
    )
  (:import 
    [java.io File]
    [javax.imageio ImageIO]
    [java.awt.image BufferedImage]
    [boofcv.struct.image ImageBase ImageUInt8 MultiSpectral]
    [boofcv.core.image ConvertBufferedImage]
    [boofcv.gui.binary VisualizeBinaryData]
    [eye_boof.core Image]
    )
  )

(set! *warn-on-reflection* true)
(set! *unchecked-math* true)

(defn is-buffImg?
  [obj]
  (instance? BufferedImage obj))

(defn argb<-intcolor
  "Convert the 32 bits color to ARGB. It returns a vector [a r g b]."
  [color]
  (vector 
    (bit-and (bit-shift-right color 24) 0xff) 
    (bit-and (bit-shift-right color 16) 0xff) 
    (bit-and (bit-shift-right color 8) 0xff) 
    (bit-and color 0xff)))

(defn r<-intcolor 
  "Returns the red value from a ARGB integer."
  ^long [^long color]
  (bit-and (bit-shift-right color 16) 0xff))

(defn g<-intcolor 
  "Returns the green value from a ARGB integer."
  ^long [^long color]
  (bit-and (bit-shift-right color 8) 0xff))

(defn b<-intcolor 
  "Returns the blue value from a ARGB integer."
  ^long [^long color]
  (bit-and color 0xff))

(defn intcolor<-argb
  "Converts the components ARGB to a 32 bits integer color."
  [a r g b]
  (bit-or (bit-shift-left (int a) 24)
          (bit-or (bit-shift-left (int r) 16)
                  (bit-or (bit-shift-left (int g) 8) (int b)))))

(defn get-raster-array
  "Returns the primitive array of a BufferedImage."
  [^BufferedImage buff]
  (.getDataElements (.getRaster buff) 0 0 (.getWidth buff) (.getHeight buff) nil))

(defn load-file-buffImg
  ^BufferedImage [^String filepath]
  (ImageIO/read (File. filepath)))

(defn to-img
  "Retuns an eye-boof Image from a given BufferedImage."
  [^BufferedImage buff]
  (let [img (ConvertBufferedImage/convertFromMulti buff nil ImageUInt8)]
    (ConvertBufferedImage/orderBandsIntoRGB img buff)
    (c/make-image 
      img
      (condp contains? (.getType buff)
        #{BufferedImage/TYPE_INT_RGB, BufferedImage/TYPE_3BYTE_BGR
          BufferedImage/TYPE_INT_BGR}
        :rgb
        ;;;;
        #{BufferedImage/TYPE_4BYTE_ABGR, BufferedImage/TYPE_INT_ARGB}
        :argb))))

(defn load-file-image
  "Returns a RGB Image from a file image."
  [^String filepath]
  (-> (load-file-buffImg filepath)
      (to-img)))

(defn to-buffered-image
  "Converts an ARGB Image to a BufferedImage."
  ^BufferedImage [img]
  (let [^ImageBase b (:mat img)]
    (case (c/get-type img)
      :argb
      (throw (Exception.  "Not implemented yet in boofcV"))
      :rgb
      (ConvertBufferedImage/convertTo_U8 b nil)
      :gray
      (ConvertBufferedImage/convertTo b nil)
      :bw
      (VisualizeBinaryData/renderBinary b nil))))

(defn create-buffered-image
  (^BufferedImage [width height] 
   (create-buffered-image width height BufferedImage/TYPE_INT_RGB))
  (^BufferedImage [width height c-type]
   (BufferedImage. width height c-type)))

(defn save-to-file!
  "Saves an image into a file. The default extension is PNG."
  ([img filepath] (save-to-file! img filepath "png"))
  ([img ^String filepath ^String ext]
   (-> (to-buffered-image img)
       (ImageIO/write ext (File. filepath)))))


(defn- new-frame
  "Creates a new frame for viewing the images."
  []
  (w/frame :title "Image Viewer" ))

(defonce frame (atom (new-frame)))

(defn view 
  "Shows the images on a grid-panel window."
  [& imgs]
  (let [buff-imgs (map #(if (instance? java.awt.image.BufferedImage %)
                          %
                          (to-buffered-image %))
                       imgs)
        n-imgs (count imgs)
        img-col 6 
        grid (w/grid-panel
               :border 5
               :hgap 10 :vgap 10
               :columns (min img-col n-imgs)
               :items (map #(w/label :icon %) buff-imgs))]
    (doto @frame
      (.setContentPane grid)
      w/pack!
      w/show!)))

(defn view-new
  "View the images in a new frame"
  [& imgs]
  (reset! frame (new-frame))
  (apply view imgs))

(def view* view-new)
