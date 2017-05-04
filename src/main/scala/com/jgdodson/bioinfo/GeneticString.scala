package com.jgdodson.bioinfo

import com.jgdodson.bioinfo.rosalind.Lexf

/**
  *
  * It's weird how the types work out just right... (Curiously Recurring Template Pattern)
  *
  * @tparam T
  */
abstract class GeneticString[T <: GeneticString[T]] {

  val seq: String


  /**
    *
    * @param i The index of the character to return
    * @return
    */
  def apply(i: Int): Char = seq(i)


  /**
    * The number of base pairs in this genetic string
    *
    * @return
    */
  def length: Int = seq.length


  // The order of the characters is significant as it is used as a default
  // ordering when manipulating sequences.
  protected def alphabet: Seq[Char]

  protected def masses: Map[Char, Double]

  def mass: Double = seq.foldLeft(0.0)(_ + masses(_))

  def reverse: T

  def substring(start: Int, end: Int): T


  /**
    * Indicate whether this GeneticString contains a spliced version of the given motif
    *
    * @param motif
    * @return
    */
  def containsSplicedMotif(motif: T): Boolean = {

    var rest = 0

    for (char <- motif.seq) {
      rest = this.seq.indexOf(char, rest) + 1

      if (rest == 0) {
        return false
      }
    }

    true
  }


  // TODO: convert this into an explicit motif finding function
  def failureArray: Vector[Int] = {

    (1 until seq.length).foldLeft((Vector[Int](0), Set[Int](0))) { (acc, next) =>
      val updated = acc._2.filter(i => seq(next) == seq(i)).map(i => i + 1) + 0
      (acc._1 :+ updated.max, updated)
    }._1
  }


  /**
    * Find all occurrences of the given motif within this genetic string
    *
    * Based on the Knuth-Morris-Pratt algorithm.
    *
    * @param motif
    * @return
    */
  def findMotif(motif: T): Vector[Int] = {

    (0 until seq.length).foldLeft((Vector[Int](), Set[Int](0))) { (acc, next) =>
      val updated = acc._2.filter(i => seq(next) == motif.seq(i)).map(_ + 1) + 0

      if (updated.max == motif.length) (acc._1 :+ (next - updated.max + 1), updated - updated.max)
      else (acc._1, updated)
    }._1
  }

  /**
    * Alternate implementation for testing purposes
    *
    * @param motif
    * @return
    */
  def findMotif2(motif: T): Vector[Int] = {

    val candidates = collection.mutable.Set[Int](0)
    val matches = collection.mutable.Seq[Int]()

    for (i <- 0 until seq.length) {
      candidates.filter(j => seq(i) == motif.seq(j)).map(_ + 1) + 0

      if (candidates.max == motif.length) {
        matches :+ (i - motif.length + 1)
      }
    }

    // Return the vector of matching indices
    matches.toVector
  }

  // TODO: Write a Regex-based motif finder. Compare speeds.

  /**
    *
    * @param k
    * @return
    */
  def kmerComposition(k: Int): Seq[Int] = {

    val indices = Lexf.enumerateFixed(k, alphabet).zipWithIndex.toMap

    (0 to length - k).foldLeft(for (_ <- Seq.range(0, indices.size)) yield 0) { (acc, next) =>
      val index = indices(seq.substring(next, next + k))
      acc.updated(index, acc(index) + 1)
    }
  }


  /**
    * Compute the Hamming distance between this genetic string and another
    *
    * Allows for strings of different length
    *
    * @param other The other genetic string
    * @return
    */
  def hammingDistance(other: T): Int = {
    val min = Math.min(length, other.length)
    val max = Math.max(length, other.length)

    (0 until min).count(i => seq(i) != other.seq(i)) + (max - min)
  }


  /**
    * Finds the starting index of all spliced occurrences of the given motif
    *
    * @param motif
    * @return
    */
  def findSplicedMotif(motif: T): Option[Vector[Int]] = {

    val res = (0 until seq.length).foldLeft(motif.seq, Vector[Int]()) { (acc, i) =>
      if (acc._1.isEmpty) acc
      else if (seq(i) == acc._1.head) (acc._1.tail, acc._2 :+ i)
      else acc
    }

    if (res._1.isEmpty) Some(res._2)
    else None
  }


  /**
    * Find one of the longest shared spliced motifs
    *
    * TODO: This should return a T
    *
    * @param other
    * @return
    */
  def longestSharedSplicedMotif(other: T): String = {

    val minSeq = if (this.length == other.length) this else List(this, other).minBy(_.length)
    val maxSeq = if (this.length == other.length) other else List(this, other).maxBy(_.length)

    val t = Vector.fill(minSeq.length)(collection.mutable.Set[Set[Int]]())

    for ((char, i) <- minSeq.seq.zipWithIndex) {

      val x = maxSeq.seq.indexOf(char)

      if (x != -1) {
        t(i).add(Set(x))
      }

      // Update all previous SSM candidates
      for (j <- 0 until i) {
        for (ssm <- t(j)) {

          val x = maxSeq.seq.indexOf(char, ssm.max + 1)
          if (x != -1) {
            t(j).add(ssm + x)
          }
        }
      }
    }

    val q = t.map(_.maxBy(_.size)).maxBy(_.size)
    q.toVector.sorted.map(i => maxSeq(i)).mkString
  }
}