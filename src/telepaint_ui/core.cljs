(ns telepaint-ui.core
  (:use [jayq.core :only [$ css html on]])
  (:require [cognitect.transit :as t]
            [jayq.core :as jq]
            [goog.dom :as dom]))

(def r (t/reader :json-verbose))
;; makes sure to transmit as object instead of
;; as array (transit tries to use its own format if you
;; use :json instead of :json-verbose
(def w (t/writer :json-verbose))

(defn log [m]
  (.log js/console m))

(def websocket* (atom nil))
(def host* (atom true))
(def current-color* (atom "#000000"))
(def mouse-down* (atom false))
(def canvas (dom/getElement "mainCanvas"))

(defn update-host [x y size color]
  (let [host-req
        {"method" "update-host",
         "data" {"x" x,
                 "y" y,
                 "size" size,
                 "color" (str "#"color)}}
        clients-req
        {"method" "update-subs"}]
    (.send @websocket* (t/write w host-req))
    (.send @websocket* (t/write w clients-req))))

(defn bind-events [canvas]
  (set! (.-context canvas) (.getContext canvas "2d"))
  (let [ctx (.-context canvas)]
    (set! (.-onmousedown canvas) (fn [e] (set! (.-isDrawing canvas) true)))
    (set! (.-onmouseup canvas) (fn [e] (set! (.-isDrawing canvas) false)))
    (set! (.-onmousemove canvas)
          (fn [e]
            (let [pos {:x (.-offsetX e)
                       :y (.-offsetY e)}]
              (when (.-isDrawing canvas)z
                (update-host (pos :x) (pos :y) 3 @current-color*)))))))
                
(defn get-context [canvas]
  (.-context canvas))

(defn fix-position [e g-canvas-elem]
  (let [pagex (.-pageX e)
        pagey (.-pageY e)]
    {:x (- pagex (.-offsetLeft g-canvas-elem))
     :y (- pagey (.-offsetTop g-canvas-elem))}))

(defn draw-line-on-canvas [canvas color x-pos y-pos]
  (let [context (.getContext canvas "2d")]
    (aset context "fillStyle" color)
    (. context beginPath)
    (. context moveTo x-pos y-pos)
    (.arc context x-pos y-pos 3 0 6.28 false)
    (. context fill)))

;; socket message handlers

(defn msg-update-host [res]
  (when @host*
    (log "updating host") ;; after this, do some canvas stuff
    (let [data (res "data")]
      (draw-line-on-canvas canvas
                           (data "color")
                           (data "x")
                           (data "y")))))

(defn msg-canvas [res]
  (when @host*
    (log "send the server my canvas")
    (let [req {"method" "host-canvas",
               "data" (.toDataURL canvas)}]
      (.send @websocket* (t/write w req)))))

(defn msg-copy-canvas [res]
  (when-not @host*
    (let [ctx (.-context canvas)
          img (new js/Image)
          data (res "data")]
      (set! (.-src img) data)
      (set! (.-onload img)
            (fn [] (.drawImage ctx img 0 0))))))

(defn msg-setup [res]
  (let [data (res "data")]
    (do
      (if-not data (reset! host* false)
              (do
                (reset! host* true)
                (log "i am host now!"))))))


;; socket event handlers
(defn socket-onopen [e]
  (let [req {"method" "setup"}]
    (.send @websocket* (t/write w req))))

(defn socket-onmessage [m]
  (let [res (t/read r (.-data m))]
    (condp = (res "method")
      "setup"       (msg-setup res)
      "update-host" (msg-update-host res)
      "canvas"      (msg-canvas res)
      "copy-canvas" (msg-copy-canvas res))))

;; main driver.
;; binds socket events, binds canvas events
(defn- main []
  (reset! websocket* (js/WebSocket. "ws://localhost:8080/intellipaint"))
  (bind-events canvas)
  (set! (.-onchange (dom/getElement "color-input"))
        (fn [e]
          (reset! current-color* (.-value (dom/getElement "color-input")))
          (log @current-color*)))
  (doall
   (map #(aset @websocket* (:event %) (:fn %))
        [{:event "onopen"
          :fn socket-onopen}
         {:event "onclose"
          :fn (fn [] (log "socket closed"))}
         {:event "onmessage"
          :fn socket-onmessage}])))

;; like $(document).ready(fn..)
(.ready ($ js/document)
        (fn []
          (main)))
