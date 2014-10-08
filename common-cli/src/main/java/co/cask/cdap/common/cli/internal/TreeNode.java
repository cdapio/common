/*
 * Copyright © 2012-2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.common.cli.internal;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in a tree.
 *
 * @param <T> type of object that the tree contains
 */
public class TreeNode<T> {

  private final T data;
  private final ArrayList<TreeNode<T>> children;
  private final TreeNode<T> parent;

  public TreeNode(T data, TreeNode<T> parent) {
    this.data = data;
    this.parent = parent;
    this.children = new ArrayList<TreeNode<T>>();
  }

  public TreeNode() {
    this(null, null);
  }

  /**
   * Finds a child node.
   *
   * @param childData the child to find
   * @return the child node with the data that equals childData
   */
  public TreeNode<T> findChild(T childData) {
    for (TreeNode<T> candidate : children) {
      if (childData.equals(candidate.data)) {
        return candidate;
      }
    }
    return null;
  }

  /**
   * Finds a child node, and if it doesn't exist yet, create it before returning.
   * @param childData the child to find
   * @return the child node with the data that equals childData
   */
  public TreeNode<T> findOrCreateChild(T childData) {
    for (TreeNode<T> candidate : children) {
      if (childData.equals(candidate.data)) {
        return candidate;
      }
    }
    return addChild(childData);
  }

  public TreeNode<T> addChild(T data) {
    TreeNode<T> result = new TreeNode<T>(data, this);
    children.add(result);
    return result;
  }

  public T getData() {
    return data;
  }

  public TreeNode<T> getParent() {
    return parent;
  }

  public List<TreeNode<T>> getChildren() {
    return children;
  }

  @Override
  public String toString() {
    return "TreeNode{" + data + '}';
  }
}
