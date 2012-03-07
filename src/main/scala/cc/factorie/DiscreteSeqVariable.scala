/* Copyright (C) 2008-2010 University of Massachusetts Amherst,
   Department of Computer Science.
   This file is part of "FACTORIE" (Factor graphs, Imperative, Extensible)
   http://factorie.cs.umass.edu, http://code.google.com/p/factorie/
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */

package cc.factorie

import scala.collection.mutable.ArrayBuffer
import scala.math
import java.util.Arrays

abstract class DiscreteSeqDomain extends Domain[Seq[DiscreteValue]] {
  def elementDomain: DiscreteDomain
}

trait DiscreteSeqVar extends IndexedSeqVar[DiscreteValue] {
  def domain: DiscreteSeqDomain
  def intValue(seqIndex:Int): Int
  def intValues: Array[Int]
  def uniqueIntValues: Array[Int]
}

//abstract class DiscreteSeqVariable extends MutableVar with cc.factorie.util.ProtectedIntArrayBuffer with SeqEqualsEq[DiscreteValue] with VarAndElementType[DiscreteSeqVariable,DiscreteValue] 
abstract class DiscreteSeqVariable extends MutableVar with cc.factorie.util.ProtectedIntArrayBuffer with DiscreteSeqVar {
  def this(initialValue:Seq[Int]) = { this(); /*_setCapacity(if (initialValue.length > 0) initialValue.length else 1);*/ if (initialValue.length > 0) _appendAll(initialValue.toArray) }
  def this(len:Int) = { this(); _setCapacity(len); _appendAll(Array.fill(len)(0)) }
  def length = _length
  def apply(index: Int): ElementType = domain.elementDomain.getValue(_apply(index))
  override def iterator = new Iterator[ElementType] {
    var i = 0
    def hasNext = i < _length
    def next = { i += 1; domain.elementDomain.getValue(_apply(i-1)) }
  }
  def domain: DiscreteSeqDomain
  def value: Value = new IndexedSeq[ElementType] {
    private val arr = new Array[ElementType](_length)
    _mapToArray(arr, (i:Int) => domain.elementDomain.getValue(i)) // Do this so that it stays constant even if _array changes later
    def length = arr.length
    def apply(i:Int) = arr(i)
   //_toSeq.map(i => domain.elementDomain.getValue(i)) // TODO make this more efficient 
  }
  def set(newValue:Value)(implicit d:DiffList): Unit = _set(Array.tabulate(newValue.length)(i => newValue(i).intValue))
  def trimCapacity: Unit = _trimCapacity
  def clear(): Unit = _clear()
  def fill(newValue:Int): Unit = Arrays.fill(_array, newValue)
  def appendInt(i:Int): Unit = _append(i)
  def +=(e:VariableType#ElementType): Unit = appendInt(e.intValue)
  def ++=(es:Iterable[VariableType#ElementType]): Unit = _appendAll(es.map(_.intValue))
  def appendInts(xs:Iterable[Int]) = _appendAll(xs)
  def intValue(seqIndex:Int): Int = _apply(seqIndex)
  def intValues: Array[Int] = _array
  def uniqueIntValues: Array[Int] = _array.distinct.sorted
  def set(seqIndex:Int, newValue:Int)(implicit d:DiffList): Unit = {
    require(d eq null)
    _update(seqIndex, newValue)
  }
}

trait SeqBreaks {
  /** Contains indices of the sequence positions which immediately follow breaks (e.g. removed stopwords) */
  val breaks = new scala.collection.mutable.BitSet
}

class CategoricalSeqDomain[C] extends DiscreteSeqDomain with Domain[Seq[CategoricalValue[C]]] {
  lazy val elementDomain: CategoricalDomain[C] = new CategoricalDomain[C]
}
abstract class CategoricalSeqVariable[C] extends DiscreteSeqVariable with VarAndElementType[CategoricalSeqVariable[C],CategoricalValue[C]] {
  def this(initialValue:Seq[C]) = {
    this()
    _setCapacity(if (initialValue.length > 0) initialValue.length else 1)
    val d = domain.elementDomain
    initialValue.foreach(c => this += d.getValue(c))
  }
  def domain: CategoricalSeqDomain[C]
  def appendCategory(x:C): Unit = this += domain.elementDomain.getValue(x)
  def appendCategories(xs:Iterable[C]): Unit = _appendAll(xs.map(c => domain.elementDomain.index(c)).toArray)
  def categoryValue(seqIndex:Int): C = domain.elementDomain.getCategory(_apply(seqIndex))
  def categoryValues: Seq[C] = Seq.tabulate(length)(i => categoryValue(i))
}
