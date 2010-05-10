/* Copyright (C) 2008-2010 Univ of Massachusetts Amherst, Computer Science Dept
   This file is part of "FACTORIE" (Factor graphs, Imperative, Extensible)
   http://factorie.cs.umass.edu, http://code.google.com/p/factorie/
   This software is provided under the terms of the Eclipse Public License 1.0
   as published by http://www.opensource.org.  For further information,
   see the file `LICENSE.txt' included with this distribution. */

package cc.factorie

import scala.collection.mutable.{ArrayBuffer, HashMap, HashSet, ListBuffer, FlatHashTable}
import scala.reflect.Manifest
import scala.util.Random
import scala.Math
import scala.util.Sorting
import scalala.tensor.Vector
import scalala.tensor.dense.DenseVector
import scalala.tensor.sparse.{SparseVector, SparseBinaryVector, SingletonBinaryVector}
import cc.factorie.util.{Log}

/**A variable whose value is a set of other variables */
abstract class SetVariable[X]() extends Variable with TypedValues {
  type ValueType = X
  type VariableType <: SetVariable[X];
  private val _members = new HashSet[X];
  def members: scala.collection.Set[X] = _members
  def size = _members.size
  def contains(x:X) = _members.contains(x)
  def add(x: X)(implicit d: DiffList): Unit = if (!_members.contains(x)) {
    if (d != null) d += new SetVariableAddDiff(x)
    _members += x
  }
  def remove(x: X)(implicit d: DiffList): Unit = if (_members.contains(x)) {
    if (d != null) d += new SetVariableRemoveDiff(x)
    _members -= x
  }
  case class SetVariableAddDiff(added: X) extends Diff {
    // Console.println ("new SetVariableAddDiff added="+added)
    def variable: SetVariable[X] = SetVariable.this
    def redo = _members += added //if (_members.contains(added)) throw new Error else
    def undo = _members -= added
  }
  case class SetVariableRemoveDiff(removed: X) extends Diff {
    //        Console.println ("new SetVariableRemoveDiff removed="+removed)
    def variable: SetVariable[X] = SetVariable.this
    def redo = _members -= removed
    def undo = _members += removed //if (_members.contains(removed)) throw new Error else
    override def toString = "SetVariableRemoveDiff of " + removed + " from " + SetVariable.this
  }
}

