package com.googlecode.scalascriptengine

import java.io.File

import com.googlecode.scalascriptengine.classloading.ClassLoaderConfig

/**
 * this holds the configuration for the script engine. Source and output paths,
 * compilation class paths, classloading classpaths . In the future it will hold scala compiler
 * settings, error reporters and so on.
 *
 * @param sourcePaths            this  is where the source files and target class directories are located
 *                               each source folder is compiled and in turn is used as classpath for the next source folder.
 *                               hence source folders must be in order of dependency, with the root classes been the
 *                               first element of the list
 * @param compilationClassPaths  this is the classpath for compilation and must be provided.
 *                               <br/>i.e. ScalaScriptEngine.currentClassPath
 * @param classLoadingClassPaths this is an extra class loading classpath.
 *                               <br/>i.e. the script folder might
 *                               utilize extra jars. Also the parent classloader will be used
 *                               to find any unresolved classes. This means that all classes visible to
 *                               your application will also be visible to the scripts even if the
 *                               classLoadingClassPaths is empty
 */
case class Config(sourcePaths: List[SourcePath],
                  compilationClassPaths: Set[File] = ScalaScriptEngine.currentClassPath,
                  classLoadingClassPaths: Set[File] = Set.empty,
                  classLoaderConfig: ClassLoaderConfig = ClassLoaderConfig.Default,
                  compilationListeners: List[CodeVersion => Unit] = Nil,
                  parentClassLoader: ClassLoader = getClass.getClassLoader) {

  if (sourcePaths.flatMap(_.sources).toSet.size < sourcePaths.size) throw new IllegalArgumentException("duplicate source directories for " + sourcePaths)
  if (sourcePaths.map(_.targetDir).toSet.size < sourcePaths.size) throw new IllegalArgumentException("duplicate target directories for " + sourcePaths)

  // a convenient constructor to create a config with the default options
  // and one only source folder.
  def this(sourcePath: File) = this(List(SourcePath(Set(sourcePath))))

  val scalaSourceDirs = sourcePaths.flatMap(_.sources)
  val targetDirs = sourcePaths.map(_.targetDir)
}

/**
 * scala source folder along with the destination class folder
 *
 * @param sources   root folders of scala sources or scala files
 * @param targetDir the outputDir, this is where all compiled classes will be stored.
 *                  Please use with care! A folder in the temp directory will usually do.
 */
case class SourcePath(sources: Set[File],
                      targetDir: File = ScalaScriptEngine.tmpOutputFolder) {
  if (!targetDir.isDirectory) throw new IllegalArgumentException(targetDir + " is not a directory")
}

object SourcePath {
  def apply(source: File): SourcePath = SourcePath(Set(source))
}