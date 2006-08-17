/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.xbean.naming.context;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.InvalidNameException;
import javax.naming.LinkRef;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.OperationNotSupportedException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * @version $Rev: 355877 $ $Date: 2005-12-10 18:48:27 -0800 (Sat, 10 Dec 2005) $
 */
public abstract class AbstractUnmodifiableContext extends AbstractContext implements Context, ContextFactory, Serializable {
    private static final long serialVersionUID = 3808693663629444493L;

    protected AbstractUnmodifiableContext(String nameInNamespace) {
        super(nameInNamespace);
    }

    protected final void addBinding(Name name, Object obj, boolean rebind) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    protected final void removeBindings(Name name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    //
    //  Lookup Binding
    //

    /**
     * Gets the object bound to the name.  The name may contain slashes.
     * @param name the name
     * @return the object bound to the name, or null if not found
     */
    protected Object getDeepBinding(String name) {
        return null;
    }

    /**
     * Gets the object bound to the name.  The name will not contain slashes.
     * @param name the name
     * @return the object bound to the name, or null if not found
     */
    protected Object getBinding(String name) throws NamingException {
        Map bindings = getBindings();
        return bindings.get(name);
    }

    /**
     * Finds the specified entry.  Normally there is no need to override this method; instead you should
     * simply implement the getDeepBindings(String) and getBindings(String) method.
     *
     * This method will follow links except for the final element which is always just returned without
     * inspection.  This means this method can be used to implement lookupLink.
     *
     * @param stringName the string version of the name; maybe null
     * @param parsedName the parsed name; may be null
     * @return the value bound to the name
     * @throws NamingException if no value is bound to that name or if a problem occurs during the lookup
     */
    protected Object lookup(String stringName, Name parsedName) throws NamingException {
        if (stringName == null && parsedName == null) {
            throw new IllegalArgumentException("Both stringName and parsedName are null");
        }
        if (stringName == null) stringName = parsedName.toString();

        // try to look up the name directly (this is the fastest path)
        Object directLookup = getDeepBinding(stringName);
        if (directLookup != null) {
            return ContextUtil.resolve(stringName, directLookup);
        }

        // if the parsed name has no parts, they are asking for the current context
        if (parsedName == null) parsedName = getNameParser().parse(stringName);
        if (parsedName.size() == 0) {
            return this;
        }


        // we didn't find an entry, pop the first element off the parsed name and attempt to
        // get a context from the bindings and delegate to that context
        Object localValue;
        String firstNameElement = parsedName.get(0);
        if (firstNameElement.length() == 0) {
            // the element is null... this is normally caused by looking up with a trailing '/' character
            localValue = this;
        } else {
            localValue = getBinding(firstNameElement);
        }

        if (localValue != null) {

            // if the name only had one part, we've looked up everything
            if (parsedName.size() == 1) {
                localValue = ContextUtil.resolve(stringName, localValue);
                return localValue;
            }

            // if we have a link ref, follow it
            if (localValue instanceof LinkRef) {
                LinkRef linkRef = (LinkRef) localValue;
                localValue = lookup(linkRef.getLinkName());
            }

            // we have more to lookup so we better have a context object
            if (!(localValue instanceof Context)) {
                throw new NameNotFoundException(stringName);
            }

            // delegate to the sub-context
            return ((Context) localValue).lookup(parsedName.getSuffix(1));
        }

        // if we didn't find an entry, it may be an absolute name
        if (stringName.indexOf(':') > 0) {
            Context ctx = new InitialContext();
            return ctx.lookup(parsedName);
        }
        throw new NameNotFoundException(stringName);
    }

    //
    //  List Bindings
    //

    /**
     * Gets a map of the bindings for the current node (i.e., no names with slashes).
     * This method must not return null.
     */
    protected Map getBindings() throws NamingException {
        throw new OperationNotSupportedException("This context is not listable");
    }

    protected NamingEnumeration list() throws NamingException {
        Map bindings = getBindings();
        return new ContextUtil.ListEnumeration(bindings);
    }

    protected NamingEnumeration listBindings() throws NamingException {
        Map bindings = getBindings();
        return new ContextUtil.ListBindingEnumeration(bindings);
    }

    //
    //  Add Binding
    //

    /**
     * Add the binding to the context.  The name will not contain a path but the value may
     * be a nested context which should be indexed for indexing contexts.
     * @param name name under which the value should be bound
     * @param value the value to bind
     * @throws NamingException if a problem occurs during the bind such as a value already being bound
     */
    protected void addBinding(String name, Object value) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    protected void addDeepBinding(String name, Object value) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");
        if (value == null) throw new NullPointerException("value is null");

        Name compoundName = ContextUtil.parseName(name);
        if (compoundName.isEmpty()) {
            throw new InvalidNameException("Name is empty");
        }

        AbstractUnmodifiableContext currentContext = this;
        for (int i = 0; i < compoundName.size(); i++) {
            String part = compoundName.get(i);

            // empty path parts are not allowed
            if (part.length() == 0) {
                throw new InvalidNameException("Name part " + i + " is empty: " + name);
            }

            // Is this the last element in the name?
            if (i == compoundName.size() - 1) {
                // we're at the end... bind the value into the parent context
                currentContext.addBinding(part, value);

                // all done... this is redundant but makes the code more readable
                break;
            } else {
                Map currentBindings = currentContext.getBindings();
                Object currentValue = currentBindings.get(part);
                if (currentValue == null) {
                    // the next step in the tree is not present, so create everything down
                    // and add it to the current bindings
                    Context context = currentContext.createContextTree(compoundName.getPrefix(i).toString(),
                            compoundName.getSuffix(i),
                            value);
                    currentContext.addBinding(part, context);

                    // all done
                    break;
                } else {
                    // the current value must be an abstract read only context
                    // todo this is a problem since a nested node could be an AbstractReadOnlyContext but not one of our contexts
                    if (!(currentValue instanceof AbstractUnmodifiableContext)) {
                        throw new NotContextException("Expected an instance of AbstractReadOnlyContext to be bound at " +
                                part + " but found an instance of " + currentValue.getClass().getName());
                    }
                    currentContext = (AbstractUnmodifiableContext) currentValue;
                    // now we recurse into the current context
                }
            }
        }
    }

    /**
     * Creates a context tree which will be rooted at the specified path and contain a single entry located down
     * a path specified by the name.  All necessary intermediate contexts will be created using the createContext method.
     * @param path the path to the context that will contains this context
     * @param name the name under which the value should be bound
     * @param value the vale
     * @return a context with the value bound at the specified name
     * @throws NamingException
     */
    protected Context createContextTree(String path, Name name, Object value) throws NamingException {
        if (path == null) throw new NullPointerException("path is null");
        if (name == null) throw new NullPointerException("name is null");
        if (name.size() < 2) throw new InvalidNameException("name must have at least 2 parts " + name);

        if (!path.endsWith("/")) path += "/";

        for (int i = name.size() - 2; i >= 0; i--) {
            String fullPath = path + name.getSuffix(i);
            String key = name.get(i + 1);
            value = createContext(fullPath, Collections.singletonMap(key, value));
        }
        return (AbstractUnmodifiableContext) value;
    }

    //
    //  Remove Binding
    //

    /**
     * Removes the binding from the context.  The name will not contain a path and the value will not
     * be a nested context although it may be a foreign context.
     * @param name name under which the value should be bound
     * @throws NamingException if a problem occurs during the bind such as a value already being bound
     */
    protected void removeBinding(String name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    protected void removeDeepBinding(String name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");

        Name compoundName = ContextUtil.parseName(name);
        if (compoundName.isEmpty()) {
            throw new InvalidNameException("Name is empty");
        }

        // we serch the tree for a target context and name to remove
        // this is normally the last context in the tree and the final name part, but
        // it may be farther up the path if the intervening nodes are empty
        AbstractUnmodifiableContext targetContext = this;
        String targetName = compoundName.get(0);

        AbstractUnmodifiableContext currentContext = this;
        for (int i = 0; i < compoundName.size(); i++) {
            String part = compoundName.get(i);

            // empty path parts are not allowed
            if (part.length() == 0) {
                throw new InvalidNameException("Name part " + i + " is empty: " + name);
            }

            Map currentBindings = currentContext.getBindings();

            // update targets
            if (currentBindings.size() > 1) {
                targetContext = currentContext;
                targetName = part;
            }


            // Is this the last element in the name?
            if (i == compoundName.size() - 1) {
                // we're at the end... bind the value into the parent context
                targetContext.removeBinding(targetName);

                // all done... this is redundant but makes the code more readable
                break;
            } else {
                Object currentValue = currentBindings.get(part);
                if (currentValue == null) {
                    // path not found we are done
                    break;
                } else {
                    // the current value must be an abstract read only context
                    // todo this is a problem since a nested node could be an AbstractReadOnlyContext but not one of our contexts
                    if (!(currentValue instanceof AbstractUnmodifiableContext)) {
                        throw new NotContextException("Expected an instance of AbstractReadOnlyContext to be bound at " +
                                part + " but found an instance of " + currentValue.getClass().getName());
                    }
                    currentContext = (AbstractUnmodifiableContext) currentValue;
                    // now we recurse into the current context
                }
            }
        }
    }

    // ==================================================================================
    // =================== Final Methods (see methods above) ============================
    // ==================================================================================

    //
    //  Lookup methods
    //

    public final Object lookup(String name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");

        Object value = lookup(name, null);

        // if we got a link back we need to resolve it
        if (value instanceof LinkRef) {
            LinkRef linkRef = (LinkRef) value;
            value = lookup(linkRef.getLinkName());
        }
        return value;
    }

    public final Object lookup(Name name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");

        Object value = lookup(null, name);

        // if we got a link back we need to resolve it
        if (value instanceof LinkRef) {
            LinkRef linkRef = (LinkRef) value;
            value = lookup(linkRef.getLinkName());
        }

        return value;
    }

    public final Object lookupLink(String name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");

        return lookup(name, null);
    }

    public final Object lookupLink(Name name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");

        return lookup(null, name);
    }

    //
    //  List Operations
    //

    public final NamingEnumeration list(String name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");

        Object o = lookup(name);
        if (o == this) {
            return list();
        } else if (o instanceof Context) {
            return ((Context) o).list("");
        } else {
            throw new NotContextException();
        }
    }

    public final NamingEnumeration listBindings(String name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");

        Object o = lookup(name);
        if (o == this) {
            return listBindings();
        } else if (o instanceof Context) {
            return ((Context) o).listBindings("");
        } else {
            throw new NotContextException();
        }
    }

    public final NamingEnumeration list(Name name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");

        Object o = lookup(name);
        if (o == this) {
            return list();
        } else if (o instanceof Context) {
            return ((Context) o).list("");
        } else {
            throw new NotContextException();
        }
    }

    public final NamingEnumeration listBindings(Name name) throws NamingException {
        if (name == null) throw new NullPointerException("name is null");

        Object o = lookup(name);
        if (o == this) {
            return listBindings();
        } else if (o instanceof Context) {
            return ((Context) o).listBindings("");
        } else {
            throw new NotContextException();
        }
    }

    //
    //  Unsupported Operations
    //

    public final void bind(Name name, Object obj) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void bind(String name, Object obj) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void close() throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final Context createSubcontext(Name name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final Context createSubcontext(String name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void destroySubcontext(Name name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void destroySubcontext(String name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void rebind(Name name, Object obj) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void rebind(String name, Object obj) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void rename(Name oldName, Name newName) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void rename(String oldName, String newName) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void unbind(Name name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }

    public final void unbind(String name) throws NamingException {
        throw new OperationNotSupportedException("Context is read only");
    }
}