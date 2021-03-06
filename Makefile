PYBABEL=.venv/bin/pybabel
I18NEXT=./node_modules/.bin/i18next-conv
LOCALE_FOLDER=./src/client/i18n
BABEL_CFG=$(LOCALE_FOLDER)/babel.cfg
TEMPLATE_FILE=$(LOCALE_FOLDER)/messages.pot
LANGS=sl en fr de it ca_ES pl pt es
JS_FILESPATH=./src/client/js/otp
JS_FILES = $(shell find $(JS_FILESPATH)/ -name '*.js')
LOCALE_FILES = $(shell find $(LOCALE_FOLDER)/ -name '*.po')
LAN=sl_SI

.PHONY: all
all: $(LOCALE_FILES)

.PHONY: update
update: $(TEMPLATE_FILE)

#Extracts new translation from JS files and creates PO template
$(TEMPLATE_FILE): $(JS_FILES)
	$(PYBABEL) extract --project=OpenTripPlanner -F $(BABEL_CFG) -s -k _tr -c TRANSLATORS: -o $(TEMPLATE_FILE) $(JS_FILESPATH)

#Updates translations with new unstraslated strings from template
.PHONY: update_po
update_po: $(LOCALE_FILES)

$(LOCALE_FILES): $(TEMPLATE_FILE)
	for LAN in $(LANGS); do $(PYBABEL) update --domain "$$LAN" --locale "$$LAN" --input-file $(TEMPLATE_FILE) --output-file $(LOCALE_FOLDER)/"$$LAN.po"; done

#Updates js files from new translations in po files
.PHONY: update_js
update_js: $(LOCALE_FILES)
	for LAN in $(LANGS); do $(I18NEXT) -l "$$LAN" -s "$(LOCALE_FOLDER)/$$LAN.po" -t "$(JS_FILESPATH)/locale/$$LAN.json"; done
	touch update_js

#Creates new translation with LAN culture info
.PHONY: init
init:
	#$(PYBABEL) init --domain "$(LAN)" --locale "$(LAN)" --input-file $(TEMPLATE_FILE) --output-file $(LOCALE_FOLDER)/"$(LAN).po";
	msginit -l "$(LAN)" -i $(TEMPLATE_FILE) -o "$(LOCALE_FOLDER)/$(LAN).po";

graphs/default/vvs-with-shapes.gtfs.zip:
	mkdir -p graphs/default
	wget https://gtfs.mfdz.de/hbg.merged.gtfs.zip -O graphs/default/vvs-with-shapes.gtfs.zip

graphs/default/stuttgart.pbf:
	mkdir -p graphs/default
	wget http://download.geofabrik.de/europe/germany/baden-wuerttemberg/stuttgart-regbez-latest.osm.pbf -O $@

graphs/ulm/tuebingen.osm.pbf:
	mkdir -p graphs/ulm
	wget http://download.geofabrik.de/europe/germany/baden-wuerttemberg/tuebingen-regbez-latest.osm.pbf -O $@

build-herrenberg: graphs/default/vvs-with-shapes.gtfs.zip graphs/default/stuttgart.pbf
	java -jar otp.jar --build ./graphs/default

build-ulm: graphs/ulm/tuebingen.osm.pbf
	java -jar otp.jar --build ./graphs/ulm

run:
	ASTAR_STATISTICS=true java -server -Dmaven.javadoc.skip=true -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:8000 -jar otp.jar --server --basePath ./ --router default --insecure

run-ulm:
	java -server -Dmaven.javadoc.skip=true -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:8000 -jar otp.jar --server --basePath ./ --router ulm --insecure

rebuild:
	mvn package -DskipTests -Dmaven.javadoc.skip=true
	cp target/otp-*-shaded.jar ./otp.jar

clean:
	rm -rf graphs/default/*.zip
	rm -rf graphs/default/*.pbf

patch-herrenberg: graphs/default/stuttgart.pbf
	cp ../gtfs-hub/config/osm/diversions.xml graphs/default/
	osmium extract --polygon src/test/resources/herrenberg/herrenberg-and-around.geojson graphs/default/stuttgart.pbf -o graphs/default/herrenberg.osm.pbf --overwrite
	docker run -v `pwd`/graphs/default/:/osm mfdz/osmosis:0.47-1-gd370b8c4 --read-pbf /osm/herrenberg.osm.pbf --tt file=/osm/diversions.xml stats=/osm/alzental_diversion.log --write-pbf /osm/herrenberg-patched.osm.pbf
	sudo chown -R `whoami`:`whoami` graphs/default
	cp graphs/default/herrenberg-patched.osm.pbf src/test/resources/herrenberg/herrenberg-hindenburg-under-construction.osm.pbf
	rm graphs/default/herrenberg*.osm.pbf
