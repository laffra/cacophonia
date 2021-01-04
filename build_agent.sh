set -e

pwd
OUTPUT_JARNAME=cacophonia.1.0.0.jar
JAVASSIST_JARNAME=../javassist.3.27.0.jar

cd bin
jar xf $JAVASSIST_JARNAME
jar cmf manifest.txt $OUTPUT_JARNAME cacophonia javassist
cp $OUTPUT_JARNAME ~
echo "Updated Cacophonia Agent jar. See:"
ls -l ~/$OUTPUT_JARNAME