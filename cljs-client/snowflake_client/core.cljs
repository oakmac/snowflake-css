(ns snowflake-client.core
  (:require
    [cljs.reader :refer [read-string]]
    [clojure.string :refer [blank? join lower-case split]]
    [goog.string :as gstring]
    [rum.core :as rum]
    [snowflake-client.util :refer [by-id js-log log]]))

;;------------------------------------------------------------------------------
;; Misc
;;------------------------------------------------------------------------------

(defn- split-classname [c]
  (let [v (split c "-")]
    {:full-classname c
     :name (join "-" (pop v))
     :hash (str "-" (peek v))}))

;;------------------------------------------------------------------------------
;; Data
;;------------------------------------------------------------------------------

(def dummy-class-data
  {"container-53f43"
    {:classname "container-53f43"
     :count 3
     :definition {"display" "flex"
                  "max-width" "400px"
                  "overflow" "auto"}
     :files [{:filename "src-cljs/my_proj/bar.cljs", :count 1}
             {:filename "src-cljs/my_proj/biz.cljs", :count 1}
             {:filename "src-cljs/my_proj/foo.cljs", :count 1}]
     :instances [{:filename "src-cljs/my_proj/bar.cljs"
                  :lines []}]}

   "label-4a5cc"
     {:classname "label-4a5cc"
      :count 22
      :definition {"color" "#aaa"
                   "display" "block"
                   "font-size" "11px"
                   "font-weight" "600"
                   "margin" "15px 0"
                   "text-transform" "uppercase"}
      :files
        [{:filename "src-cljs/my_proj/bar.cljs", :count 5}
         {:filename "src-cljs/my_proj/biz.cljs", :count 2}
         {:filename "src-cljs/my_proj/foo.cljs", :count 3}
         {:filename "templates/athens.mustache", :count 1}
         {:filename "templates/chalcis.mustache", :count 1}
         {:filename "templates/chania.mustache", :count 1}
         {:filename "templates/heraklion.mustache", :count 1}
         {:filename "templates/ioannina.mustache", :count 1}
         {:filename "templates/larissa.mustache", :count 1}
         {:filename "templates/patras.mustache", :count 1}
         {:filename "templates/rhodes.mustache", :count 1}
         {:filename "templates/thessaloniki.mustache", :count 1}
         {:filename "templates/volos.mustache", :count 1}]}

   "header-label-f23ea"
     {:classname "header-label-f23ea"
      :count 6
      :definition {"color" "#999"
                   "font-size" "14px"
                   "font-weight" "600"
                   "text-transform" "uppercase"}
      :files [{:filename "src-cljs/my_proj/bar.cljs", :count 2}
              {:filename "src-cljs/my_proj/biz.cljs", :count 2}
              {:filename "src-cljs/my_proj/foo.cljs", :count 2}]}

   "list-label-27ddb"
     {:classname "list-label-27ddb"
      :count 8
      :definition {"font-family" "\"Open Sans Light\""
                   "font-weight" "300"
                   "font-size" "34px"
                   "border" "2px solid orange"
                   "padding" "10px 20px"}
      :files [{:filename "templates/larissa.mustache", :count 8}]}

   "clr-7f0e5"
     {:classname "clr-7f0e5"
      :count 41
      :definition {"clear" "both"}
      :files
        [{:filename "src-cljs/my_proj/biz.cljs", :count 41}]}})

(def dummy-single-class (get dummy-class-data "clr-7f0e5"))

(def dummy-files-list
  ["src-cljs/my_proj/bar.cljs"
   "src-cljs/my_proj/biz.cljs"
   "src-cljs/my_proj/foo.cljs"
   "templates/athens.mustache"
   "templates/chalcis.mustache"
   "templates/chania.mustache"
   "templates/heraklion.mustache"
   "templates/ioannina.mustache"
   "templates/larissa.mustache"
   "templates/patras.mustache"
   "templates/rhodes.mustache"
   "templates/thessaloniki.mustache"
   "templates/volos.mustache"])

(def all-classes (atom dummy-class-data))

(defn- alpha-sort [a b]
  (compare (:classname a) (:classname b)))

(defn- add-classname [[classname c]]
  (assoc c :classname classname))

(defn- all-classes-sorted-by-name []
  (->> @all-classes
       (map add-classname)
       (sort alpha-sort)
       (into [])))

;;------------------------------------------------------------------------------
;; Data
;;------------------------------------------------------------------------------



;;------------------------------------------------------------------------------
;; App State
;;------------------------------------------------------------------------------

(def initial-app-state
  {:active-tab :classes
   :flakes-list-search-txt ""
   :active-project nil
   :projects nil
   :socket-connected? false
   :sort-classes-by "a-z"
   :selected-class dummy-single-class})

(def app-state (atom initial-app-state))

;;------------------------------------------------------------------------------
;; Events
;;------------------------------------------------------------------------------

(defn- on-change-input [js-evt]
  (let [new-text (aget js-evt "currentTarget" "value")]
    (swap! app-state assoc :search-text new-text)))

(defn- mark-active [active-idx idx itm]
  (if (= active-idx idx)
    (assoc itm :active? true)
    itm))

(defn- on-mouse-enter-class-row [js-evt]
  (let [target-el (aget js-evt "currentTarget")
        active-idx (int (.getAttribute target-el "data-idx"))
        classname (.getAttribute target-el "data-classname")
        current-class-list (:classes @app-state)
        class-list-with-no-active (map #(dissoc % :active?) current-class-list)
        new-class-list (map-indexed (partial mark-active active-idx) class-list-with-no-active)]
    ;; defensive - make sure the class exists
    (when (get @all-classes classname false)
      (swap! app-state assoc :classes (into [] new-class-list)
                             :files (get-in @all-classes [classname :files])
                             :selected-classname classname))))

;;------------------------------------------------------------------------------
;; Components
;;------------------------------------------------------------------------------

(rum/defc FileView < rum/static
  []
  [:div.fileview-col-c79be "*shows how the classname is used in the file*"])

(rum/defc FileRow < rum/static
  [f]
  [:div.file-row-c9a43 (:filename f)])

(rum/defc FilesList < rum/static
  [[selected-classname files]]
  [:div.files-col-74a77
    [:h4.col-title-2c774 "Files"
      [:div.count-13cac (str (count files) " files contain " selected-classname)]]
    ;; (str "Files that contain: " selected-classname)
    [:div.list-wrapper-7462e
      (map FileRow files)]])

(rum/defc MainInput < rum/static
  [search-text]
  [:input.main-input-f14b8 {
                            :on-change on-change-input
                            :placeholder "Search classes and files"
                            :type "text"
                            :value search-text}])

(defn- change-class-search [js-evt]
  (let [new-text (aget js-evt "currentTarget" "value")]
    (swap! app-state assoc :flakes-list-search-txt new-text)))

(defn- on-change-classes-sort-by [js-evt]
  (let [new-value (aget js-evt "currentTarget" "value")]
    (swap! app-state assoc :sort-classes-by new-value)))

(defn- click-new-class-btn []
  (swap! app-state assoc :new-class-modal-showing? true))

(rum/defc UtilBody < rum/static
  []
  [:div "TODO: Util Body"])

(rum/defc FilesBody < rum/static
  []
  [:div "TODO: Files Body"])

(defn- sort-first [a b]
  (compare (first a) (first b)))

(rum/defc PropertyListItem < rum/static
  [[key val]]
  [:li.property-s9fe7
    [:span.key-s2dab (str key ":")]
    [:span.val-s1b79 val]])

(rum/defc ClassDefinitionList < rum/static
  [d]
  (let [v (sort sort-first (into [] d))]
    [:ul.definition-list-s7860
      (map PropertyListItem v)]))

;; TODO: figure out how this should work...
(def default-preview-styles {})

(rum/defc ClassPreview < rum/static
  [d]
  [:div.preview-wrapper-sb56f
    [:div {:style (merge d default-preview-styles)} "Lorem ipsum"]])

(rum/defc ClassInstance < rum/static
  [instance]
  [:div "TODO: instance"])

(def active-file-idx 2)

(rum/defc FileRow2 < rum/static
  [idx filename]
  [:li {:class (str "file-sbc22" (when (= idx active-file-idx) " active-se4ea"))}
    filename])

(def dummy-text
  ["</div>"
   ""
   "<button class=\"<?php if (user.isActive) { 'active-s9aae' } ?>\">"
   "  Save Now"
   "</button>"])

;; NOTE: this component should show a preview of the text where the class was found
;;       in the file. with things greyed out except for the class name
;; TODO:
;; - line numbers
;; - highlight the class name
(rum/defc TextLine < rum/static
  [line]
  (js/React.createElement "div" (js-obj "dangerouslySetInnerHTML"
                                  (js-obj "__html" (str "<div class='code-line-se969'>"
                                                        (gstring/htmlEscape line)
                                                        "</div>")))))

(rum/defc FilesView < rum/static
  []
  [:div.flex-container-s6d73
    [:div.files-container-s0cbd
      [:ul.files-list-sf03b
        (map-indexed FileRow2 dummy-files-list)]]
    [:div.text-container-sd0e9
      (map TextLine dummy-text)]])

(rum/defc ClassDetailBody < rum/static
  [c]
  (let [split-classname (split-classname (:classname c))]
    [:div.right
      [:h2.class-name-sb0bc
        [:span (:name split-classname)]
        [:span.muted-sff17 (:hash split-classname)]
        [:span.link-s4dda "rename"]]
      ;; TODO: make "files" be a dashed underline
      [:p (str "Used " (:count c) " times in " (count (:files c)) " ")
        [:span {:style {:text-decoration "underline"}} "files"]
        ". Found on " [:code "<button>"] " elements."]
      [:div.flex-container-s6d73
        [:div.definition-s7e19
          [:h4.header-s63ca "Definition"]
          (ClassDefinitionList (:definition c))]
        [:div.preview-s376a
          [:h4.header-s63ca "Preview"]
          (ClassPreview (:definition c))]]
      [:div
        [:h4.header-s63ca "Usage" [:span.small-link-sf848 "see all"]]
        (FilesView)]]))
        ;; TODO: create a view where all of the instances are listed
        ;;       you can scroll down the page and see the files

(rum/defc ClassesSortByOptions < rum/static
  [sort-by]
  [:select
    {:on-change on-change-classes-sort-by
     :value sort-by}
    [:option {:value "a-z"} "A-Z"]
    [:option {:value "z-a"} "Z-A"]
    ; [:option {:value "files-a-z"} "Files A-Z"]
    ; [:option {:value "files-z-a"} "Files Z-A"]
    [:option {:value "most-used"} "Most Used"]
    [:option {:value "least-used"} "Least Used"]])

(defn- click-class-list-item [classname]
  (when-let [new-class (get @all-classes classname)]
    (swap! app-state assoc :selected-class new-class)))

(rum/defc ClassListItem < rum/static
  [[classname c]]
  (let [split-classname (split-classname classname)]
    [:li {:on-click (partial click-class-list-item classname)}
      [:div.classname-sfb8e
        [:span (:name split-classname)]
        [:span.muted-s5ce7 (:hash split-classname)]]
      [:div.small-count-s3096
        (str (:count c) " instances, ")
        (count (:files c)) " files"]]))

(rum/defc ClassList < rum/static
  [classes]
  [:ul.class-list-sfe2c
    (map ClassListItem classes)])

(defn- match-text? [search-txt class]
  (not= -1 (.indexOf (first class) search-txt)))

(defn- class-name-comp [a b]
  (compare (first a) (first b)))

(defn- usage-comp [a b]
  (compare (-> b second :count) (-> a second :count)))

(def sort-fns
  {"a-z" class-name-comp
   "z-a" class-name-comp
   "most-used" usage-comp
   "least-used" usage-comp})

(def reverse-sort-methods #{"z-a" "least-used"})

;; TODO: probably should memoize this
(defn- filtered-and-sorted-classlist [search-txt sort-method]
  (let [lowercase-search-txt (lower-case search-txt)
        filter-fn (partial match-text? lowercase-search-txt)
        sort-fn (get sort-fns sort-method)
        reverse? (contains? reverse-sort-methods sort-method)
        classes (->> @all-classes
                     (filter filter-fn)
                     (sort sort-fn))]
    (if reverse?
      (reverse classes)
      classes)))

(rum/defc LeftPanel < rum/static
  [{:keys [flakes-list-search-txt sort-classes-by]}]
  (let [classes (filtered-and-sorted-classlist flakes-list-search-txt sort-classes-by)]
    [:div.left
      [:input.search-input-s24fa
        {:on-change change-class-search
         :placeholder "Search Flakes"
         :type "text"
         :value flakes-list-search-txt}]
      [:label.sort-by-s7ff8 "Sort By:"
        (ClassesSortByOptions sort-classes-by)]
      (if (empty? classes)
        [:div.no-results-s993d "No flakes found"]
        (ClassList classes))]))

(rum/defc MainBody < rum/static
  [state]
  [:div
    ;; [:button.primary-s04e4 {:on-click click-new-class-btn} "New Flake"]
    [:div.flex-container-s6d73
      (LeftPanel state)
      (if (:selected-class state)
        (ClassDetailBody (:selected-class state))
        [:div "TODO: no class selected; prevent me from happening"])]])

(rum/defc Tabs < rum/static
  [active-tab]
  [:ul.tabs-sd691
    [:li [:a {:class (when (= active-tab :classes) "active-sb974")
              :href "#/classes"} "Classes"]]
    [:li [:a {:class (when (= active-tab :files) "active-sb974")
              :href "#/files"} "Files"]]
    [:li [:a {:class (when (= active-tab :util) "active-sb974")
              :href "#/util"} "Utilities"]]])

(defn- on-change-project-select [js-evt]
  (let [project-path (aget js-evt "currentTarget" "value")]
    (swap! app-state assoc :active-project project-path)))

(rum/defc ProjectOption < rum/static
  [{:keys [name path]}]
  [:option {:value path} name])

(rum/defc Header < rum/static
  [active-project-path projects]
  [:header
    [:h1.title-5ac1e "Snowflake CSS"]
    [:div.tagline-1119f "they're all special snowflakes..."]
    [:select {:on-change on-change-project-select
              :value active-project-path}
      (map ProjectOption projects)]])

(rum/defc Body2 < rum/static
  [{:keys [name]}]
  [:h2 name])

;; TODO: style this
(rum/defc EstablishingConnectionScreen < rum/static
  []
  [:div "Establishing socket connection..."])

;; TODO: style this
(rum/defc LoadingProjectsScreen < rum/static
  []
  [:div "Loading projects..."])

(defn- condensed-projects [state]
  (->> state
       :projects
       vals
       (map #(select-keys % #{:name :path}))))

(rum/defc SnowflakeAppBody < rum/static
  [state]
  [:div.container-53f43
    (Header (:active-project state) (condensed-projects state))
    (MainBody state)])

(rum/defc SnowflakeApp < rum/static
  [state]
  (cond
    (not (:socket-connected? state))
    (EstablishingConnectionScreen)

    (not (:projects state))
    (LoadingProjectsScreen)

    ;; TODO: socket disconnected / reconnecting screens

    :else
    (SnowflakeAppBody state)))

;;------------------------------------------------------------------------------
;; Render Loop
;;------------------------------------------------------------------------------

(def app-container-el (by-id "appContainer"))

(defn- on-change-app-state
  [_kwd _the-atom _old-state new-state]
  (rum/mount (SnowflakeApp new-state) app-container-el))

;; render the page every time the state changes
(add-watch app-state :render-loop on-change-app-state)

;;------------------------------------------------------------------------------
;; Socket Events
;;------------------------------------------------------------------------------

(def localhost
  (str (aget js/location "protocol")
       "//"
       (aget js/location "host")))

(defn- on-socket-connection []
  (swap! app-state assoc :socket-connected? true))

(defn- receive-state-from-server [new-state-edn]
  (let [new-state (read-string new-state-edn)]
    (swap! app-state assoc :projects new-state)))

(defn- connect-socket-io! []
  (let [socket (.connect js/io localhost)]
    (.on socket "connect" on-socket-connection)
    (.on socket "new-state" receive-state-from-server)))

;;------------------------------------------------------------------------------
;; Global App Init
;;------------------------------------------------------------------------------

(defn- init!
  "Global app init."
  []
  (connect-socket-io!)
  ;; trigger page render
  (swap! app-state identity))


(.addEventListener js/window "load" init!)
