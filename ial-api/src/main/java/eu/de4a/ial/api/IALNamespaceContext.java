/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.de4a.ial.api;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Singleton;
import com.helger.xml.namespace.MapBasedNamespaceContext;

/**
 * XML Namespace context for IAL
 *
 * @author Philip Helger
 */
@Singleton
public class IALNamespaceContext extends MapBasedNamespaceContext
{
  private static final class SingletonHolder
  {
    static final IALNamespaceContext INSTANCE = new IALNamespaceContext ();
  }

  protected IALNamespaceContext ()
  {
    addMapping ("ial", CIALJAXB.NAMESPACE_URI_IAL);
  }

  @Nonnull
  public static IALNamespaceContext getInstance ()
  {
    return SingletonHolder.INSTANCE;
  }
}
