// FormInputProcessing.java



class FormInputProcessing<T> {
    private Form<T> form;

    public FormInputProcessing(Form<T> form) {
        this.form = form;
    }

    public final void clearInput()
    {
        // Visit all the (visible) form components and clear the input on each.
        visitFormComponentsPostOrder(new FormComponent.AbstractVisitor()
        {
            @Override
            public void onFormComponent(final FormComponent<?> formComponent)
            {
                if (formComponent.isVisibleInHierarchy())
                {
                    // Clear input from form component
                    formComponent.clearInput();
                }
            }
        });
    }

    public void inputChanged() {
        // Implementation for handling input change
    }
}

// FormValidation.java
class FormValidation<T> {
    private Form<T> form;

    public FormValidation(Form<T> form) {
        this.form = form;
    }

    public void validateComponents()
    {
        visitFormComponentsPostOrder(new ValidationVisitor()
        {
            @Override
            public void validate(final FormComponent<?> formComponent)
            {
                final Form<?> form = formComponent.getForm();
                if (form == Form.this && form.isEnabledInHierarchy() && form.isVisibleInHierarchy())
                {
                    formComponent.validate();
                }
            }
        });
    }

    public void validateFormValidator(final IFormValidator validator)
    {
        if (validator == null)
        {
            throw new IllegalArgumentException("Argument [[validator]] cannot be null");
        }

        final FormComponent<?>[] dependents = validator.getDependentFormComponents();

        boolean validate = true;

        if (dependents != null)
        {
            for (int j = 0; j < dependents.length; j++)
            {
                final FormComponent<?> dependent = dependents[j];
                // check if the dependent component is valid
                if (!dependent.isValid())
                {
                    validate = false;
                    break;
                }
                // check if the dependent component is visible and is attached to
                // the page
                else if (!isFormComponentVisibleInPage(dependent))
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("IFormValidator in form `" +
                                getPageRelativePath() +
                                "` depends on a component that has been removed from the page or is no longer visible. " +
                                "Offending component id `" + dependent.getId() + "`.");
                    }
                    validate = false;
                    break;
                }
            }
        }

        if (validate)
        {
            validator.validate(this);
        }
    }

    public void validateFormValidators()
    {
        final int count = formValidators_size();
        for (int i = 0; i < count; i++)
        {
            validateFormValidator(formValidators_get(i));
        }
    }

    /**
     * Validates {@link FormComponent}s as well as {@link IFormValidator}s in nested {@link Form}s.
     *
     * @see #validate()
     */
    private void validateNestedForms()
    {
        visitChildren(Form.class, new IVisitor<Form<?>>()
        {
            public Object component(Form<?> form)
            {
                if (form.isEnabledInHierarchy() && form.isVisibleInHierarchy())
                {
                    form.validateComponents();
                    form.validateFormValidators();
                    form.onValidate();
                    return CONTINUE_TRAVERSAL;
                }
                return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
            }
        });
    }
    {
        visitChildren(Form.class, new IVisitor<Form<?>>()
        {
            public Object component(Form<?> form)
            {
                if (form.isEnabledInHierarchy() && form.isVisibleInHierarchy())
                {
                    form.validateComponents();
                    form.validateFormValidators();
                    form.onValidate();
                    return CONTINUE_TRAVERSAL;
                }
                return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
            }
        });
    }
}
class FormConfigurator<T> {
    private Form<T> form;

    public FormConfigurator(Form<T> form) {
        this.form = form;
    }

    public void setMaxSize(final Bytes maxSize)
    {
        this.maxSize = maxSize;
    }

    public void setMultiPart(boolean multiPart)
    {
        if (multiPart)
        {
            this.multiPart |= MULTIPART_HARD;
        }
        else
        {
            this.multiPart &= ~MULTIPART_HARD;
        }
    }
    /**
     * Gets the default IFormSubmittingComponent. If set (not null), a hidden submit component will
     * be rendered right after the form tag, so that when users press enter in a textfield, this
     * submit component's action will be selected. If no default component is set (it is null),
     * nothing additional is rendered.
     * <p>
     * WARNING: note that this is a best effort only. Unfortunately having a 'default' button in a
     * form is ill defined in the standards, and of course IE has it's own way of doing things.
     * </p>
     * There can be only one default submit component per form hierarchy. So if you want to get the
     * default component on a nested form, it will actually delegate the call to root form. </b>
     *
     * @return The submit component to set as the default IFormSubmittingComponent, or null when you
     *         want to 'unset' any previously set default IFormSubmittingComponent
     */
    public final IFormSubmittingComponent getDefaultButton()
    {
        if (isRootForm())
        {
            return defaultSubmittingComponent;
        }
        else
        {
            return getRootForm().getDefaultButton();
        }
    }
    public void setDefaultButton(IFormSubmittingComponent submittingComponent)
    {
        if (isRootForm())
        {
            defaultSubmittingComponent = submittingComponent;
        }
        else
        {
            getRootForm().setDefaultButton(submittingComponent);
        }
    }

    public final Component setVersioned(final boolean isVersioned)
    {
        super.setVersioned(isVersioned);

        // Search for FormComponents like TextField etc.
        visitFormComponents(new FormComponent.AbstractVisitor()
        {
            @Override
            public void onFormComponent(final FormComponent<?> formComponent)
            {
                formComponent.setVersioned(isVersioned);
            }
        });
        return this;
    }
}

class FormSubmissionHandler<T> {
    private Form<T> form;

    public FormSubmissionHandler(Form<T> form) {
        this.form = form;
    }

    public void onFormSubmitted()
    {
        markFormsSubmitted();

        if (handleMultiPart())
        {
            // Tells FormComponents that a new user input has come
            inputChanged();

            String url = getRequest().getParameter(getHiddenFieldId());
            if (!Strings.isEmpty(url))
            {
                dispatchEvent(getPage(), url);
            }
            else
            {
                // First, see if the processing was triggered by a Wicket IFormSubmittingComponent
                final IFormSubmittingComponent submittingComponent = findSubmittingButton();

                // When processing was triggered by a Wicket IFormSubmittingComponent and that
                // component indicates it wants to be called immediately
                // (without processing), call IFormSubmittingComponent.onSubmit() right away.
                if (submittingComponent != null && !submittingComponent.getDefaultFormProcessing())
                {
                    submittingComponent.onSubmit();
                }
                else
                {
                    // this is the root form
                    Form<?> formToProcess = this;

                    // find out whether it was a nested form that was submitted
                    if (submittingComponent != null)
                    {
                        formToProcess = submittingComponent.getForm();
                    }

                    // process the form for this request
                    formToProcess.process(submittingComponent);
                }
            }
        }
        // If multi part did fail check if an error is registered and call
        // onError
        else if (hasError())
        {
            callOnError();
        }
    }

    public void process(IFormSubmittingComponent submittingComponent)
    {
        // save the page in case the component is removed during submit
        final Page page = getPage();
        String hiddenFieldId = getHiddenFieldId();

        // process the form for this request
        if (process())
        {
            // let clients handle further processing
            delegateSubmit(submittingComponent);
        }

        // WICKET-1912
        // If the form is stateless page parameters contain all form component
        // values. We need to remove those otherwise they get appended to action URL
        final PageParameters parameters = page.getPageParameters();
        if (parameters != null)
        {
            visitFormComponents(new FormComponent.IVisitor()
            {
                public Object formComponent(IFormVisitorParticipant formComponent)
                {
                    if (formComponent instanceof FormComponent)
                    {
                        parameters.remove(((FormComponent<?>)formComponent).getInputName());
                    }

                    return Component.IVisitor.CONTINUE_TRAVERSAL;
                }
            });
            parameters.remove(hiddenFieldId);
            if (submittingComponent instanceof AbstractSubmitLink)
            {
                AbstractSubmitLink submitLink = (AbstractSubmitLink)submittingComponent;
                parameters.remove(submitLink.getInputName());
            }
        }
    }
    public boolean process()
    {
        if (!isEnabledInHierarchy() || !isVisibleInHierarchy())
        {
            // since process() can be called outside of the default form workflow, an additional
            // check is needed
            return false;
        }

        // run validation
        validate();

        // If a validation error occurred
        if (hasError())
        {
            // mark all children as invalid
            markFormComponentsInvalid();

            // let subclass handle error
            callOnError();

            // Form has an error
            return false;
        }
        else
        {
            // mark all children as valid
            markFormComponentsValid();

            // before updating, call the interception method for clients
            beforeUpdateFormComponentModels();

            // Update model using form data
            updateFormComponentModels();

            onValidateModelObjects();
            if (hasError())
            {
                callOnError();
                return false;
            }

            // Persist FormComponents if requested
            persistFormComponentData();

            // Form has no error
            return true;
        }
    }

    /**
     * Gets the IFormSubmittingComponent which submitted this form.
     *
     * @return The component which submitted this form, or null if the processing was not triggered
     *         by a registered IFormSubmittingComponent
     */
    public final IFormSubmittingComponent findSubmittingButton()
    {
        IFormSubmittingComponent submittingComponent = (IFormSubmittingComponent)getPage().visitChildren(
                IFormSubmittingComponent.class, new IVisitor<Component>()
                {
                    public Object component(final Component component)
                    {
                        // Get submitting component
                        final IFormSubmittingComponent submittingComponent = (IFormSubmittingComponent)component;
                        final Form<?> form = submittingComponent.getForm();

                        // Check for component-name or component-name.x request string
                        if ((form != null) && (form.getRootForm() == Form.this))
                        {
                            String name = submittingComponent.getInputName();
                            if ((getRequest().getParameter(name) != null) ||
                                    (getRequest().getParameter(name + ".x") != null))
                            {
                                if (!component.isVisibleInHierarchy())
                                {
                                    throw new WicketRuntimeException("Submit Button " +
                                            submittingComponent.getInputName() + " (path=" +
                                            component.getPageRelativePath() + ") is not visible");
                                }
                                if (!component.isEnabledInHierarchy())
                                {
                                    throw new WicketRuntimeException("Submit Button " +
                                            submittingComponent.getInputName() + " (path=" +
                                            component.getPageRelativePath() + ") is not enabled");
                                }
                                return submittingComponent;
                            }
                        }
                        return CONTINUE_TRAVERSAL;
                    }
                });

        return submittingComponent;
    }

    /**
     * THIS METHOD IS NOT PART OF THE WICKET API. DO NOT ATTEMPT TO OVERRIDE OR CALL IT.
     *
     * Handles form submissions.
     *
     * @see Form#validate()
     */
    public final void onFormSubmitted()
    {
        markFormsSubmitted();

        if (handleMultiPart())
        {
            // Tells FormComponents that a new user input has come
            inputChanged();

            String url = getRequest().getParameter(getHiddenFieldId());
            if (!Strings.isEmpty(url))
            {
                dispatchEvent(getPage(), url);
            }
            else
            {
                // First, see if the processing was triggered by a Wicket IFormSubmittingComponent
                final IFormSubmittingComponent submittingComponent = findSubmittingButton();

                // When processing was triggered by a Wicket IFormSubmittingComponent and that
                // component indicates it wants to be called immediately
                // (without processing), call IFormSubmittingComponent.onSubmit() right away.
                if (submittingComponent != null && !submittingComponent.getDefaultFormProcessing())
                {
                    submittingComponent.onSubmit();
                }
                else
                {
                    // this is the root form
                    Form<?> formToProcess = this;

                    // find out whether it was a nested form that was submitted
                    if (submittingComponent != null)
                    {
                        formToProcess = submittingComponent.getForm();
                    }

                    // process the form for this request
                    formToProcess.process(submittingComponent);
                }
            }
        }
        // If multi part did fail check if an error is registered and call
        // onError
        else if (hasError())
        {
            callOnError();
        }
    }

    /**
     * Calls onError on this {@link Form} and any enabled and visible nested form, if the respective
     * {@link Form} actually has errors.
     */
    protected void callOnError()
    {
        onError();
        // call onError on nested forms
        visitChildren(Form.class, new IVisitor<Component>()
        {
            public Object component(Component component)
            {
                final Form<?> form = (Form<?>)component;
                if (!form.isEnabledInHierarchy() || !form.isVisibleInHierarchy())
                {
                    return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
                }
                if (form.hasError())
                {
                    form.onError();
                }
                return IVisitor.CONTINUE_TRAVERSAL;
            }
        });
    }

    /**
     * Sets FLAG_SUBMITTED to true on this form and every enabled nested form.
     */
    private void markFormsSubmitted()
    {
        setFlag(FLAG_SUBMITTED, true);

        visitChildren(Form.class, new IVisitor<Component>()
        {
            public Object component(Component component)
            {
                Form<?> form = (Form<?>)component;
                if (form.isEnabledInHierarchy() && isVisibleInHierarchy())
                {
                    form.setFlag(FLAG_SUBMITTED, true);
                    return IVisitor.CONTINUE_TRAVERSAL;
                }
                return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
            }
        });
    }

    public final IFormSubmittingComponent findSubmittingButton()
    {
        IFormSubmittingComponent submittingComponent = (IFormSubmittingComponent)getPage().visitChildren(
                IFormSubmittingComponent.class, new IVisitor<Component>()
                {
                    public Object component(final Component component)
                    {
                        // Get submitting component
                        final IFormSubmittingComponent submittingComponent = (IFormSubmittingComponent)component;
                        final Form<?> form = submittingComponent.getForm();

                        // Check for component-name or component-name.x request string
                        if ((form != null) && (form.getRootForm() == Form.this))
                        {
                            String name = submittingComponent.getInputName();
                            if ((getRequest().getParameter(name) != null) ||
                                    (getRequest().getParameter(name + ".x") != null))
                            {
                                if (!component.isVisibleInHierarchy())
                                {
                                    throw new WicketRuntimeException("Submit Button " +
                                            submittingComponent.getInputName() + " (path=" +
                                            component.getPageRelativePath() + ") is not visible");
                                }
                                if (!component.isEnabledInHierarchy())
                                {
                                    throw new WicketRuntimeException("Submit Button " +
                                            submittingComponent.getInputName() + " (path=" +
                                            component.getPageRelativePath() + ") is not enabled");
                                }
                                return submittingComponent;
                            }
                        }
                        return CONTINUE_TRAVERSAL;
                    }
                });

        return submittingComponent;
    }

    /**
     * Handles multi-part processing of the submitted data.
     *
     * WARNING
     *
     * If this method is overridden it can break {@link FileUploadField}s on this form
     *
     * @return false if form is multipart and upload failed
     */
    protected boolean handleMultiPart()
    {
        if (isMultiPart())
        {
            // Change the request to a multipart web request so parameters are
            // parsed out correctly
            try
            {
                final WebRequest multipartWebRequest = ((WebRequest)getRequest()).newMultipartWebRequest(getMaxSize());
                getRequestCycle().setRequest(multipartWebRequest);
            }
            catch (WicketRuntimeException wre)
            {
                if (wre.getCause() == null || !(wre.getCause() instanceof FileUploadException))
                {
                    throw wre;
                }

                FileUploadException e = (FileUploadException)wre.getCause();

                // Create model with exception and maximum size values
                final Map<String, Object> model = new HashMap<String, Object>();
                model.put("exception", e);
                model.put("maxSize", getMaxSize());

                onFileUploadException((FileUploadException)wre.getCause(), model);

                // don't process the form if there is a FileUploadException
                return false;
            }
        }
        return true;
    }

    /**
     * Method for dispatching/calling a interface on a page from the given url. Used by
     * {@link org.apache.wicket.markup.html.form.Form#onFormSubmitted()} for dispatching events
     *
     * @param page
     *            The page where the event should be called on.
     * @param url
     *            The url which describes the component path and the interface to be called.
     */
    private void dispatchEvent(final Page page, final String url)
    {
        RequestCycle rc = RequestCycle.get();
        IRequestCycleProcessor processor = rc.getProcessor();
        final RequestParameters requestParameters = processor.getRequestCodingStrategy().decode(
                new FormDispatchRequest(rc.getRequest(), url));
        IRequestTarget rt = processor.resolve(rc, requestParameters);
        if (rt instanceof IListenerInterfaceRequestTarget)
        {
            IListenerInterfaceRequestTarget interfaceTarget = ((IListenerInterfaceRequestTarget)rt);
            interfaceTarget.getRequestListenerInterface().invoke(page, interfaceTarget.getTarget());
        }
        else
        {
            throw new WicketRuntimeException(
                    "Attempt to access unknown request listener interface " +
                            requestParameters.getInterfaceName());
        }
    }
}

class FormValidationHandler<T> {
    private Form<T> form;

    public FormValidationHandler(Form<T> form) {
        this.form = form;
    }

    public void validate()
    {
        if (isEnabledInHierarchy() && isVisibleInHierarchy())
        {
            // since this method can be called directly by users, this additional check is needed
            validateComponents();
            validateFormValidators();
            onValidate();
            validateNestedForms();
        }
    }

    public void clearInput()
    {
        // Visit all the (visible) form components and clear the input on each.
        visitFormComponentsPostOrder(new FormComponent.AbstractVisitor()
        {
            @Override
            public void onFormComponent(final FormComponent<?> formComponent)
            {
                if (formComponent.isVisibleInHierarchy())
                {
                    // Clear input from form component
                    formComponent.clearInput();
                }
            }
        });
    }

    public void markFormComponentsInvalid()
    {
        // call invalidate methods of all nested form components
        visitFormComponentsPostOrder(new FormComponent.AbstractVisitor()
        {
            @Override
            public void onFormComponent(final FormComponent<?> formComponent)
            {
                if (formComponent.isVisibleInHierarchy())
                {
                    formComponent.invalid();
                }
            }
        });
    }

    /**
     * Gets all {@link IFormValidator}s added to this form
     *
     * @return unmodifiable collection of {@link IFormValidator}s
     */
    public final Collection<IFormValidator> getFormValidators()
    {
        final int size = formValidators_size();

        List<IFormValidator> validators = null;

        if (size == 0)
        {
            // form has no validators, use empty collection
            validators = Collections.emptyList();
        }
        else
        {
            // form has validators, copy all into collection
            validators = new ArrayList<IFormValidator>(size);
            for (int i = 0; i < size; i++)
            {
                validators.add(formValidators_get(i));
            }
        }
        return Collections.unmodifiableCollection(validators);
    }

    /**
     * This generates a piece of javascript code that sets the url in the special hidden field and
     * submits the form.
     *
     * Warning: This code should only be called in the rendering phase for form components inside
     * the form because it uses the css/javascript id of the form which can be stored in the markup.
     *
     * @param url
     *            The interface url that has to be stored in the hidden field and submitted
     * @return The javascript code that submits the form.
     */
    public final CharSequence getJsForInterfaceUrl(CharSequence url)
    {
        Form<?> root = getRootForm();
        return new AppendingStringBuffer("document.getElementById('").append(
                root.getHiddenFieldId())
                .append("').value='")
                .append(url)
                .append("';document.getElementById('")
                .append(root.getMarkupId())
                .append("').submit();");
    }

    /**
     * Checks if the specified form component visible and is attached to a page
     *
     * @param fc
     *            form component
     *
     * @return true if the form component and all its parents are visible and there component is in
     *         page's hierarchy
     */
    private boolean isFormComponentVisibleInPage(FormComponent<?> fc)
    {
        if (fc == null)
        {
            throw new IllegalArgumentException("Argument `fc` cannot be null");
        }
        return fc.isVisibleInHierarchy();
    }

    /**
     * Callback during the validation stage of the form
     */
    protected void onValidate()
    {

    }

    /**
     * Called after form components have updated their models. This is a late-stage validation that
     * allows outside frameworks to validate any beans that the form is updating.
     *
     * This validation method is not preferred because at this point any errors will not unroll any
     * changes to the model object, so the model object is in a modified state potentially
     * containing illegal values. However, with external frameworks there may not be an alternate
     * way to validate the model object. A good example of this is a JSR303 Bean Validator
     * validating the model object to check any class-level constraints, in order to check such
     * constaints the model object must contain the values set by the user.
     */
    protected void onValidateModelObjects()
    {

    }

    /**
     * Mark each form component on this form invalid.
     */
    protected final void markFormComponentsInvalid()
    {
        // call invalidate methods of all nested form components
        visitFormComponentsPostOrder(new FormComponent.AbstractVisitor()
        {
            @Override
            public void onFormComponent(final FormComponent<?> formComponent)
            {
                if (formComponent.isVisibleInHierarchy())
                {
                    formComponent.invalid();
                }
            }
        });
    }


    /**
     * Mark each form component on this form and on nested forms valid.
     */
    protected final void markFormComponentsValid()
    {
        internalMarkFormComponentsValid();
        markNestedFormComponentsValid();
    }
    /**
     * Mark each form component on nested form valid.
     */
    private void markNestedFormComponentsValid()
    {
        visitChildren(Form.class, new IVisitor<Form<?>>()
        {
            public Object component(Form<?> component)
            {
                Form<?> form = component;
                if (form.isEnabledInHierarchy() && form.isVisibleInHierarchy())
                {
                    form.internalMarkFormComponentsValid();
                    return CONTINUE_TRAVERSAL;
                }
                return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
            }
        });
    }

    /**
     * Mark each form component on this form valid.
     */
    private void internalMarkFormComponentsValid()
    {
        // call valid methods of all nested form components
        visitFormComponentsPostOrder(new FormComponent.AbstractVisitor()
        {
            @Override
            public void onFormComponent(final FormComponent<?> formComponent)
            {
                if (formComponent.getForm() == Form.this && formComponent.isVisibleInHierarchy())
                {
                    formComponent.valid();
                }
            }
        });
    }

}

class FormModelManager<T> {
    private Form<T> form;

    public FormModelManager(Form<T> form) {
        this.form = form;
    }

    public void updateFormComponentModels()
    {
        internalUpdateFormComponentModels();
        updateNestedFormComponentModels();
    }

    public void loadPersistentFormComponentValues()
    {
        visitFormComponentsPostOrder(new FormComponent.AbstractVisitor()
        {
            @Override
            public void onFormComponent(final FormComponent<?> formComponent)
            {
                // Component must implement persister interface and
                // persistence for that component must be enabled.
                // Else ignore the persisted value. It'll be deleted
                // once the user submits the Form containing that FormComponent.
                // Note: if that is true, values may remain persisted longer
                // than really necessary
                if (formComponent.isPersistent() && formComponent.isVisibleInHierarchy())
                {
                    // The persister
                    final IValuePersister persister = getValuePersister();

                    // Retrieve persisted value
                    persister.load(formComponent);
                }
            }
        });
    }

    public void removePersistentFormComponentValues(final boolean disablePersistence)
    {
        // The persistence manager responsible to persist and retrieve
        // FormComponent data
        final IValuePersister persister = getValuePersister();

        // Search for FormComponents like TextField etc.
        visitFormComponentsPostOrder(new FormComponent.AbstractVisitor()
        {
            @Override
            public void onFormComponent(final FormComponent<?> formComponent)
            {
                if (formComponent.isVisibleInHierarchy())
                {
                    // remove the FormComponent's persisted data
                    persister.clear(formComponent);

                    // Disable persistence if requested. Leave unchanged
                    // otherwise.
                    if (formComponent.isPersistent() && disablePersistence)
                    {
                        formComponent.setPersistent(false);
                    }
                }
            }
        });
    }

    /**
     * Returns the root form or this, if this is the root form.
     *
     * @return root form or this form
     */
    public Form<?> getRootForm()
    {
        Form<?> form;
        Form<?> parent = this;
        do
        {
            form = parent;
            parent = form.findParent(Form.class);
        }
        while (parent != null);

        return form;
    }

    /**
     * THIS METHOD IS NOT PART OF THE WICKET PUBLIC API. DO NOT CALL IT.
     * <p>
     * Retrieves FormComponent values related to the page using the persister and assign the values
     * to the FormComponent. Thus initializing them.
     */
    public final void loadPersistentFormComponentValues()
    {
        visitFormComponentsPostOrder(new FormComponent.AbstractVisitor()
        {
            @Override
            public void onFormComponent(final FormComponent<?> formComponent)
            {
                // Component must implement persister interface and
                // persistence for that component must be enabled.
                // Else ignore the persisted value. It'll be deleted
                // once the user submits the Form containing that FormComponent.
                // Note: if that is true, values may remain persisted longer
                // than really necessary
                if (formComponent.isPersistent() && formComponent.isVisibleInHierarchy())
                {
                    // The persister
                    final IValuePersister persister = getValuePersister();

                    // Retrieve persisted value
                    persister.load(formComponent);
                }
            }
        });
    }

    /**
     * Removes already persisted data for all FormComponent children and disable persistence for the
     * same components.
     *
     * @see Page#removePersistedFormData(Class, boolean)
     *
     * @param disablePersistence
     *            if true, disable persistence for all FormComponents on that page. If false, it
     *            will remain unchanged.
     */
    public void removePersistentFormComponentValues(final boolean disablePersistence)
    {
        // The persistence manager responsible to persist and retrieve
        // FormComponent data
        final IValuePersister persister = getValuePersister();

        // Search for FormComponents like TextField etc.
        visitFormComponentsPostOrder(new FormComponent.AbstractVisitor()
        {
            @Override
            public void onFormComponent(final FormComponent<?> formComponent)
            {
                if (formComponent.isVisibleInHierarchy())
                {
                    // remove the FormComponent's persisted data
                    persister.clear(formComponent);

                    // Disable persistence if requested. Leave unchanged
                    // otherwise.
                    if (formComponent.isPersistent() && disablePersistence)
                    {
                        formComponent.setPersistent(false);
                    }
                }
            }
        });
    }

    /**
     * Visits the form's children FormComponents and inform them that a new user input is available
     * in the Request
     */
    private void inputChanged()
    {
        visitFormComponentsPostOrder(new FormComponent.AbstractVisitor()
        {
            @Override
            public void onFormComponent(final FormComponent<?> formComponent)
            {
                if (formComponent.isVisibleInHierarchy())
                {
                    formComponent.inputChanged();
                }
            }
        });
    }

    /**
     * Persist (e.g. Cookie) FormComponent data to be reloaded and re-assigned to the FormComponent
     * automatically when the page is visited by the user next time.
     *
     * @see org.apache.wicket.markup.html.form.FormComponent#updateModel()
     */
    private void persistFormComponentData()
    {
        // Cannot add cookies to request cycle unless it accepts them
        // We could conceivably be HTML over some other protocol!
        if (getRequestCycle() instanceof WebRequestCycle)
        {
            // The persistence manager responsible to persist and retrieve
            // FormComponent data
            final IValuePersister persister = getValuePersister();

            // Search for FormComponent children. Ignore all other
            visitFormComponentsPostOrder(new FormComponent.AbstractVisitor()
            {
                @Override
                public void onFormComponent(final FormComponent<?> formComponent)
                {
                    if (formComponent.isVisibleInHierarchy())
                    {
                        // If persistence is switched on for that FormComponent
                        // ...
                        if (formComponent.isPersistent())
                        {
                            // Save component's data (e.g. in a cookie)
                            persister.save(formComponent);
                        }
                        else
                        {
                            // Remove component's data (e.g. cookie)
                            persister.clear(formComponent);
                        }
                    }
                }
            });
        }
    }

    /**
     * Update the model of all components on this form and nested forms using the fields that were
     * sent with the current request. This method only updates models when the Form.validate() is
     * called first that takes care of the conversion for the FormComponents.
     *
     * Normally this method will not be called when a validation error occurs in one of the form
     * components.
     *
     * @see org.apache.wicket.markup.html.form.FormComponent#updateModel()
     */
    protected final void updateFormComponentModels()
    {
        internalUpdateFormComponentModels();
        updateNestedFormComponentModels();
    }

    /**
     * Update the model of all components on nested forms.
     *
     * @see #updateFormComponentModels()
     */
    private final void updateNestedFormComponentModels()
    {
        visitChildren(Form.class, new IVisitor<Form<?>>()
        {
            public Object component(Form<?> form)
            {
                if (form.isEnabledInHierarchy() && form.isVisibleInHierarchy())
                {
                    form.internalUpdateFormComponentModels();
                    return CONTINUE_TRAVERSAL;
                }
                return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
            }
        });
    }

    /**
     * Update the model of all components on this form.
     *
     * @see #updateFormComponentModels()
     */
    private void internalUpdateFormComponentModels()
    {
        FormComponent.visitComponentsPostOrder(this, new FormModelUpdateVisitor(this));

        MarkupContainer border = findParent(Border.class);
        if (border != null)
        {
            FormComponent.visitComponentsPostOrder(border, new FormModelUpdateVisitor(this));
        }
    }

    /**
     * Template method to allow clients to do any processing (like recording the current model so
     * that, in case onSubmit does further validation, the model can be rolled back) before the
     * actual updating of form component models is done.
     */
    protected void beforeUpdateFormComponentModels()
    {
    }

    /**
     * Gets the form component persistence manager; it is lazy loaded.
     *
     * @return The form component value persister
     */
    protected IValuePersister getValuePersister()
    {
        return new CookieValuePersister();
    }
}

class FormErrorHandler<T> {
    private Form<T> form;

    public FormErrorHandler(Form<T> form) {
        this.form = form;
    }
    /**
     * Registers an error feedback message for this component
     *
     * @param error
     *            error message
     * @param args
     *            argument replacement map for ${key} variables
     */
    public final void error(String error, Map<String, Object> args)
    {
        error(new MapVariableInterpolator(error, args).toString());
    }
    public void onError()
    {   }

    public boolean hasError()
    {
        // if this form itself has an error message
        if (hasErrorMessage())
        {
            return true;
        }

        // the form doesn't have any errors, now check any nested form
        // components
        return anyFormComponentError();
    }

    /**
     * Find out whether there is any registered error for a form component.
     *
     * @return whether there is any registered error for a form component
     */
    private boolean anyFormComponentError()
    {
        final boolean[] error = new boolean[] { false };

        // TODO not sure why we need that class. We only use the callback method.
        final IVisitor<Component> visitor = new IVisitor<Component>()
        {
            public Object component(final Component component)
            {
                if (component.hasErrorMessage())
                {
                    error[0] = true;
                    return Component.IVisitor.STOP_TRAVERSAL;
                }

                // Traverse all children
                return Component.IVisitor.CONTINUE_TRAVERSAL;
            }
        };

        // Iterator over all children and grand children. Any component may have registered an error
        // message. Do NOT restrict to Form and FormComponents.
        visitChildren(Component.class, new IVisitor<Component>()
        {
            public Object component(final Component component)
            {
                return visitor.component(component);
            }
        });

        // Borders need special treatment
        if (!error[0] && (getParent() instanceof Border))
        {
            MarkupContainer border = getParent();
            border.visitChildren(Component.class, new IVisitor<Component>()
            {
                public Object component(final Component component)
                {
                    if ((component == Form.this) || !(component instanceof FormComponent))
                    {
                        return Component.IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
                    }

                    return visitor.component(component);
                }
            });
        }

        return error[0];
    }
}

class FormComponentManager<T> {
    private Form<T> form;

    public FormComponentManager(Form<T> form) {
        this.form = form;
    }

    public void add(IFormValidator validator)
    {
        if (validator == null)
        {
            throw new IllegalArgumentException("Argument `validator` cannot be null");
        }

        // add the validator
        formValidators_add(validator);

        // see whether the validator listens for add events
        if (validator instanceof IValidatorAddListener)
        {
            ((IValidatorAddListener)validator).onAdded(this);
        }
    }

    public void remove(IFormValidator validator)
    {
        if (validator == null)
        {
            throw new IllegalArgumentException("Argument `validator` cannot be null");
        }

        IFormValidator removed = formValidators_remove(validator);
        if (removed == null)
        {
            throw new IllegalStateException(
                    "Tried to remove form validator that was not previously added. "
                            + "Make sure your validator's equals() implementation is sufficient");
        }
        addStateChange(new FormValidatorRemovedChange(removed));
    }

    /**
     * Convenient and typesafe way to visit all the form components on a form.
     *
     * @param visitor
     *            The visitor interface to call
     */
    public final void visitFormComponents(final FormComponent.IVisitor visitor)
    {
        visitChildren(FormComponent.class, new IVisitor<Component>()
        {
            public Object component(final Component component)
            {
                visitor.formComponent((FormComponent<?>)component);
                return CONTINUE_TRAVERSAL;
            }
        });

        visitChildrenInContainingBorder(visitor);
    }

    public final int formValidators_indexOf(IFormValidator validator)
    {
        if (formValidators != null)
        {
            if (formValidators instanceof IFormValidator)
            {
                final IFormValidator v = (IFormValidator)formValidators;
                if (v == validator || v.equals(validator))
                {
                    return 0;
                }
            }
            else
            {
                final IFormValidator[] validators = (IFormValidator[])formValidators;
                for (int i = 0; i < validators.length; i++)
                {
                    final IFormValidator v = validators[i];
                    if (v == validator || v.equals(validator))
                    {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    public final IFormValidator formValidators_remove(IFormValidator validator)
    {
        int index = formValidators_indexOf(validator);
        if (index != -1)
        {
            return formValidators_remove(index);
        }
        return null;
    }

    public final IFormValidator formValidators_remove(int index)
    {
        if (formValidators instanceof IFormValidator)
        {
            if (index == 0)
            {
                final IFormValidator removed = (IFormValidator)formValidators;
                formValidators = null;
                return removed;
            }
            else
            {
                throw new IndexOutOfBoundsException();
            }
        }
        else
        {
            final IFormValidator[] validators = (IFormValidator[])formValidators;
            final IFormValidator removed = validators[index];
            // check if we can collapse array of 1 element into a single object
            if (validators.length == 2)
            {
                formValidators = validators[1 - index];
            }
            else
            {
                IFormValidator[] newValidators = new IFormValidator[validators.length - 1];
                int j = 0;
                for (int i = 0; i < validators.length; i++)
                {
                    if (i != index)
                    {
                        newValidators[j++] = validators[i];
                    }
                }
                formValidators = newValidators;
            }
            return removed;
        }
    }

    /**
     * Convenient and typesafe way to visit all the form components on a form.
     *
     * @param visitor
     *            The visitor interface to call
     */
    public final void visitFormComponents(final FormComponent.IVisitor visitor)
    {
        visitChildren(FormComponent.class, new IVisitor<Component>()
        {
            public Object component(final Component component)
            {
                visitor.formComponent((FormComponent<?>)component);
                return CONTINUE_TRAVERSAL;
            }
        });

        visitChildrenInContainingBorder(visitor);
    }

    /**
     * @param validator
     *            The form validator to add to the formValidators Object (which may be an array of
     *            IFormValidators or a single instance, for efficiency)
     */
    private void formValidators_add(final IFormValidator validator)
    {
        if (formValidators == null)
        {
            formValidators = validator;
        }
        else
        {
            // Get current list size
            final int size = formValidators_size();

            // Create array that holds size + 1 elements
            final IFormValidator[] validators = new IFormValidator[size + 1];

            // Loop through existing validators copying them
            for (int i = 0; i < size; i++)
            {
                validators[i] = formValidators_get(i);
            }

            // Add new validator to the end
            validators[size] = validator;

            // Save new validator list
            formValidators = validators;
        }
    }

    /**
     * Gets form validator from formValidators Object (which may be an array of IFormValidators or a
     * single instance, for efficiency) at the given index
     *
     * @param index
     *            The index of the validator to get
     * @return The form validator
     */
    private IFormValidator formValidators_get(int index)
    {
        if (formValidators == null)
        {
            throw new IndexOutOfBoundsException();
        }
        if (formValidators instanceof IFormValidator[])
        {
            return ((IFormValidator[])formValidators)[index];
        }
        return (IFormValidator)formValidators;
    }

    /**
     * @return The number of form validators in the formValidators Object (which may be an array of
     *         IFormValidators or a single instance, for efficiency)
     */
    private int formValidators_size()
    {
        if (formValidators == null)
        {
            return 0;
        }
        if (formValidators instanceof IFormValidator[])
        {
            return ((IFormValidator[])formValidators).length;
        }
        return 1;
    }
}

class FormRenderer<T> {
    private Form<T> form;

    public FormRenderer(Form<T> form) {
        this.form = form;
    }
    /**
     * Constructs a form with no validation.
     *
     * @param id
     *            See Component
     */
    public Form(final String id)
    {
        super(id);
        setOutputMarkupId(true);
    }

    /**
     * @param id
     *            See Component
     * @param model
     *            See Component
     * @see org.apache.wicket.Component#Component(String, IModel)
     */
    public Form(final String id, IModel<T> model)
    {
        super(id, model);
        setOutputMarkupId(true);
    }
    public void onComponentTag(final ComponentTag tag)
    {
        super.onComponentTag(tag);

        checkComponentTag(tag, "form");

        if (isRootForm())
        {
            String method = getMethod().toLowerCase();
            tag.put("method", method);
            String url = urlFor(IFormSubmitListener.INTERFACE).toString();
            if (encodeUrlInHiddenFields())
            {
                int i = url.indexOf('?');
                String action = (i > -1) ? url.substring(0, i) : "";
                tag.put("action", action);
                // alternatively, we could just put an empty string here, so
                // that mounted paths stay in good order. I decided against this
                // as I'm not sure whether that could have side effects with
                // other encoders
            }
            else
            {
                tag.put("action", Strings.escapeMarkup(url));
            }

            if (isMultiPart())
            {
                tag.put("enctype", "multipart/form-data");
            }
            else
            {
                // sanity check
                String enctype = (String)tag.getAttributes().get("enctype");
                if ("multipart/form-data".equalsIgnoreCase(enctype))
                {
                    // though not set explicitly in Java, this is a multipart
                    // form
                    setMultiPart(true);
                }
            }
        }
        else
        {
            tag.setName("div");
            tag.remove("method");
            tag.remove("action");
            tag.remove("enctype");
            // see renderhead for some non-root javascript markers
        }
    }

    public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag)
    {
        if (isRootForm())
        {
            // get the hidden field id
            String nameAndId = getHiddenFieldId();

            // render the hidden field
            AppendingStringBuffer buffer = new AppendingStringBuffer(HIDDEN_DIV_START).append(
                    "<input type=\"hidden\" name=\"")
                    .append(nameAndId)
                    .append("\" id=\"")
                    .append(nameAndId)
                    .append("\" />");

            // if it's a get, did put the parameters in the action attribute,
            // and have to write the url parameters as hidden fields
            if (encodeUrlInHiddenFields())
            {
                String url = urlFor(IFormSubmitListener.INTERFACE).toString();
                int i = url.indexOf('?');
                String[] params = ((i > -1) ? url.substring(i + 1) : url).split("&");

                writeParamsAsHiddenFields(params, buffer);
            }
            buffer.append("</div>");
            getResponse().write(buffer);

            // if a default submitting component was set, handle the rendering of that
            if (defaultSubmittingComponent instanceof Component)
            {
                final Component submittingComponent = (Component)defaultSubmittingComponent;
                if (submittingComponent.isVisibleInHierarchy() &&
                        submittingComponent.isEnabledInHierarchy())
                {
                    appendDefaultButtonField(markupStream, openTag);
                }
            }
        }

        // do the rest of the processing
        super.onComponentTagBody(markupStream, openTag);
    }

    public void renderHead(IHeaderResponse response)
    {
        if (!isRootForm() && isMultiPart())
        {
            // register some metadata so we can later properly handle multipart ajax posts for
            // embedded forms
            registerJavascriptNamespaces(response);
            response.renderJavascript("Wicket.Forms[\"" + getMarkupId() + "\"]={multipart:true};",
                    Form.class.getName() + "." + getMarkupId() + ".metadata");
        }
    }

    protected void renderPlaceholderTag(ComponentTag tag, Response response)
    {
        if (isRootForm())
        {
            super.renderPlaceholderTag(tag, response);
        }
        else
        {
            // rewrite inner form tag as div
            response.write("<div style=\"display:none\"");
            if (getOutputMarkupId())
            {
                response.write(" id=\"");
                response.write(getMarkupId());
                response.write("\"");
            }
            response.write("></div>");
        }
    }

    /**
     *
     * @return true if form's method is 'get'
     */
    protected boolean encodeUrlInHiddenFields()
    {
        String method = getMethod().toLowerCase();
        return method.equals("get");
    }

    /**
     * If a default IFormSubmittingComponent was set on this form, this method will be called to
     * render an extra field with an invisible style so that pressing enter in one of the textfields
     * will do a form submit using this component. This method is overridable as what we do is best
     * effort only, and may not what you want in specific situations. So if you have specific
     * usability concerns, or want to follow another strategy, you may override this method.
     *
     * @param markupStream
     *            The markup stream
     * @param openTag
     *            The open tag for the body
     */
    protected void appendDefaultButtonField(final MarkupStream markupStream,
                                            final ComponentTag openTag)
    {

        AppendingStringBuffer buffer = new AppendingStringBuffer();

        // div that is not visible (but not display:none either)
        buffer.append(HIDDEN_DIV_START);

        // add an empty textfield (otherwise IE doesn't work)
        buffer.append("<input type=\"text\" autocomplete=\"false\"/>");

        // add the submitting component
        final Component submittingComponent = (Component)defaultSubmittingComponent;
        buffer.append("<input type=\"submit\" name=\"");
        buffer.append(defaultSubmittingComponent.getInputName());
        buffer.append("\" onclick=\" var b=document.getElementById('");
        buffer.append(submittingComponent.getMarkupId());
        buffer.append("'); if (b!=null&amp;&amp;b.onclick!=null&amp;&amp;typeof(b.onclick) != 'undefined') {  var r = b.onclick.bind(b)(); if (r != false) b.click(); } else { b.click(); };  return false;\" ");
        buffer.append(" />");

        // close div
        buffer.append("</div>");

        getResponse().write(buffer);
    }

    /**
     *
     * @param params
     * @param buffer
     */
    protected void writeParamsAsHiddenFields(String[] params, AppendingStringBuffer buffer)
    {
        for (int j = 0; j < params.length; j++)
        {
            String[] pair = params[j].split("=");

            buffer.append("<input type=\"hidden\" name=\"")
                    .append(recode(pair[0]))
                    .append("\" value=\"")
                    .append(pair.length > 1 ? recode(pair[1]) : "")
                    .append("\" />");
        }
    }

    /**
     * Take URL-encoded query string value, unencode it and return HTML-escaped version
     *
     * @param s
     *            value to reencode
     * @return reencoded value
     */
    private String recode(String s)
    {
        String un = WicketURLDecoder.QUERY_INSTANCE.decode(s);
        return Strings.escapeMarkup(un).toString();
    }

    /**
     * Returns the HiddenFieldId which will be used as the name and id property of the hiddenfield
     * that is generated for event dispatches.
     *
     * @return The name and id of the hidden field.
     */
    public final String getHiddenFieldId()
    {
        String formId;
        if (!getPage().isPageStateless())
        {
            // only assigned inside statefull pages WICKET-3438
            formId = getMarkupId();
        }
        else
        {
            formId = Form.getRootFormRelativeId(this).replace(":", "_");
        }
        return getInputNamePrefix() + formId + "_hf_0";
    }

    protected final String getJavascriptId()
    {
        return getMarkupId();
    }
}

class FormLifecycleManager <T> {
    protected void onDetach()
    {
        super.internalOnDetach();
        setFlag(FLAG_SUBMITTED, false);

        for (IFormValidator validator : getFormValidators())
        {
            if (validator != null && (validator instanceof IDetachable))
            {
                ((IDetachable)validator).detach();
            }
        }

        super.onDetach();
    }
    /**
     * Method to override if you want to do something special when an error occurs (other than
     * simply displaying validation errors).
     */
    protected void onError()
    {
    }

    protected void onBeforeRender()
    {
        // clear multipart hint, it will be set if necessary by the visitor
        this.multiPart &= ~MULTIPART_HINT;

        super.onBeforeRender();
    }

    protected void onSubmit()
    {
    }
    /**
     * Called (by the default implementation of 'process') when all fields validated, the form was
     * updated and it's data was allowed to be persisted. It is meant for delegating further
     * processing to clients.
     * <p>
     * This implementation first finds out whether the form processing was triggered by a nested
     * IFormSubmittingComponent of this form. If that is the case, that component's onSubmit is
     * called first.
     * </p>
     * <p>
     * Regardless of whether a submitting component was found, the form's onSubmit method is called
     * next.
     * </p>
     *
     * @param submittingComponent
     *            the component that triggered this form processing, or null if the processing was
     *            triggered by something else (like a non-Wicket submit button or a javascript
     *            execution)
     */
    protected void delegateSubmit(IFormSubmittingComponent submittingComponent)
    {
        // when the given submitting component is not null, it means that it was the
        // submitting component
        Form<?> formToProcess = this;
        if (submittingComponent != null)
        {
            // use the form which the submittingComponent has submitted for further processing
            formToProcess = submittingComponent.getForm();
            submittingComponent.onSubmit();
        }

        // Model was successfully updated with valid data
        formToProcess.onSubmit();

        // call onSubmit on nested forms
        formToProcess.visitChildren(Form.class, new IVisitor<Form<?>>()
        {
            public Object component(Form<?> component)
            {
                Form<?> form = component;
                if (form.isEnabledInHierarchy() && form.isVisibleInHierarchy())
                {
                    form.onSubmit();
                    return IVisitor.CONTINUE_TRAVERSAL;
                }
                return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
            }
        });
    }

    /**
     * Calls onError on this {@link Form} and any enabled and visible nested form, if the respective
     * {@link Form} actually has errors.
     */
    protected void callOnError()
    {
        onError();
        // call onError on nested forms
        visitChildren(Form.class, new IVisitor<Component>()
        {
            public Object component(Component component)
            {
                final Form<?> form = (Form<?>)component;
                if (!form.isEnabledInHierarchy() || !form.isVisibleInHierarchy())
                {
                    return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
                }
                if (form.hasError())
                {
                    form.onError();
                }
                return IVisitor.CONTINUE_TRAVERSAL;
            }
        });
    }

    /**
     * Sets FLAG_SUBMITTED to true on this form and every enabled nested form.
     */
    public void markFormsSubmitted()
    {
        setFlag(FLAG_SUBMITTED, true);

        visitChildren(Form.class, new IVisitor<Component>()
        {
            public Object component(Component component)
            {
                Form<?> form = (Form<?>)component;
                if (form.isEnabledInHierarchy() && isVisibleInHierarchy())
                {
                    form.setFlag(FLAG_SUBMITTED, true);
                    return IVisitor.CONTINUE_TRAVERSAL;
                }
                return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
            }
        });
    }

    /**
     * The default message may look like ".. may not exceed 10240 Bytes..". Which is ok, but
     * sometimes you may want something like "10KB". By subclassing this method you may replace
     * maxSize in the model or add you own property and use that in your error message.
     * <p>
     * Don't forget to call super.onFileUploadException(e, model) at the end of your method.
     *
     * @param e
     * @param model
     */
    protected void onFileUploadException(final FileUploadException e,
                                         final Map<String, Object> model)
    {
        if (e instanceof SizeLimitExceededException)
        {
            // Resource key should be <form-id>.uploadTooLarge to
            // override default message
            final String defaultValue = "Upload must be less than " + getMaxSize();
            String msg = getString(getId() + "." + UPLOAD_TOO_LARGE_RESOURCE_KEY,
                    Model.ofMap(model), defaultValue);
            error(msg);
        }
        else
        {
            // Resource key should be <form-id>.uploadFailed to override
            // default message
            final String defaultValue = "Upload failed: " + e.getLocalizedMessage();
            String msg = getString(getId() + "." + UPLOAD_FAILED_RESOURCE_KEY, Model.ofMap(model),
                    defaultValue);
            error(msg);

            log.warn(msg, e);
        }
    }

    protected void internalOnModelChanged()
    {
        // Visit all the form components and validate each
        visitFormComponentsPostOrder(new FormComponent.AbstractVisitor()
        {
            @Override
            public void onFormComponent(final FormComponent<?> formComponent)
            {
                // If form component is using form model
                if (formComponent.sameInnermostModel(Form.this))
                {
                    formComponent.modelChanged();
                }
            }
        });
    }
}

class FormSpecialized <T>{
    public static abstract class ValidationVisitor implements FormComponent.IVisitor
    {
        /**
         * @see org.apache.wicket.markup.html.form.FormComponent.IVisitor#formComponent(org.apache.wicket.markup.html.form.IFormVisitorParticipant)
         */
        public Object formComponent(IFormVisitorParticipant component)
        {
            if (component instanceof FormComponent)
            {
                FormComponent<?> formComponent = (FormComponent<?>)component;

                Form<?> form = formComponent.getForm();
                if (!form.isVisibleInHierarchy() || !form.isEnabledInHierarchy())
                {
                    // do not validate formComponent or any of formComponent's children
                    return Component.IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
                }

                if (formComponent.isVisibleInHierarchy() && formComponent.isValid() &&
                        formComponent.isEnabledInHierarchy())
                {
                    validate(formComponent);
                }
            }
            if (component.processChildren())
            {
                return Component.IVisitor.CONTINUE_TRAVERSAL;
            }
            else
            {
                return Component.IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
            }
        }

        /**
         * Callback that should be used to validate form component
         *
         * @param formComponent
         */
        public abstract void validate(FormComponent<?> formComponent);
    }

    /**
     * Visitor used to update component models
     *
     * @author Igor Vaynberg (ivaynberg)
     */
    private static class FormModelUpdateVisitor implements Component.IVisitor<Component>
    {
        private final Form<?> formFilter;

        /**
         * Constructor
         *
         * @param formFilter
         */
        public FormModelUpdateVisitor(Form<?> formFilter)
        {
            this.formFilter = formFilter;
        }

        /** {@inheritDoc} */
        public Object component(Component component)
        {
            if (component instanceof IFormModelUpdateListener)
            {
                final Form<?> form = Form.findForm(component);
                if (form != null)
                {
                    if (this.formFilter == null || this.formFilter == form)
                    {
                        if (form.isEnabledInHierarchy())
                        {
                            if (component.isVisibleInHierarchy() &&
                                    component.isEnabledInHierarchy())
                            {
                                ((IFormModelUpdateListener)component).updateModel();
                            }
                        }
                    }
                }
            }
            return Component.IVisitor.CONTINUE_TRAVERSAL;
        }
    }

    /**
     *
     */
    class FormDispatchRequest extends Request
    {
        private final Map<String, String[]> params = new HashMap<String, String[]>();

        private final Request realRequest;

        private final String url;

        /**
         * Construct.
         *
         * @param realRequest
         * @param url
         */
        public FormDispatchRequest(final Request realRequest, final String url)
        {
            this.realRequest = realRequest;
            this.url = realRequest.decodeURL(url);

            String queryString = this.url.substring(this.url.indexOf("?") + 1);
            RequestUtils.decodeUrlParameters(queryString, params);
        }

        /**
         * @see org.apache.wicket.Request#getLocale()
         */
        @Override
        public Locale getLocale()
        {
            return realRequest.getLocale();
        }

        /**
         * @see org.apache.wicket.Request#getParameter(java.lang.String)
         */
        @Override
        public String getParameter(String key)
        {
            String p[] = params.get(key);
            return p != null && p.length > 0 ? p[0] : null;
        }

        /**
         * @see org.apache.wicket.Request#getParameterMap()
         */
        @Override
        public Map<String, String[]> getParameterMap()
        {
            return params;
        }

        /**
         * @see org.apache.wicket.Request#getParameters(java.lang.String)
         */
        @Override
        public String[] getParameters(String key)
        {
            String[] param = params.get(key);
            if (param != null)
            {
                return param;
            }
            return new String[0];
        }

        /**
         * @see org.apache.wicket.Request#getPath()
         */
        @Override
        public String getPath()
        {
            return realRequest.getPath();
        }

        @Override
        public String getRelativePathPrefixToContextRoot()
        {
            return realRequest.getRelativePathPrefixToContextRoot();
        }

        @Override
        public String getRelativePathPrefixToWicketHandler()
        {
            return realRequest.getRelativePathPrefixToWicketHandler();
        }

        /**
         * @see org.apache.wicket.Request#getURL()
         */
        @Override
        public String getURL()
        {
            return url;
        }

        @Override
        public String getQueryString()
        {
            return realRequest.getQueryString();
        }
    }

    /**
     * Change object to keep track of form validator removals
     *
     * @author Igor Vaynberg (ivaynberg at apache dot org)
     */
    public class FormValidatorRemovedChange extends Change
    {
        private static final long serialVersionUID = 1L;

        private final IFormValidator removed;

        /**
         * Construct.
         *
         * @param removed
         */
        public FormValidatorRemovedChange(final IFormValidator removed)
        {
            super();
            this.removed = removed;
        }

        @Override
        public void undo()
        {
            add(removed);
        }
    }
}

class Form <T>{

    /**
     * Constructs a form with no validation.
     *
     * @param id
     *            See Component
     */
    public Form(final String id)
    {
        super(id);
        setOutputMarkupId(true);
    }

    /**
     * @param id
     *            See Component
     * @param model
     *            See Component
     * @see org.apache.wicket.Component#Component(String, IModel)
     */
    public Form(final String id, IModel<T> model)
    {
        super(id, model);
        setOutputMarkupId(true);
    }

    public String getValidatorKeyPrefix()
    {
        return null;
    }

    /**
     * Returns whether the form is a root form, which means that there's no other form in it's
     * parent hierarchy.
     *
     * @return true if form is a root form, false otherwise
     */
    public boolean isRootForm()
    {
        return findParent(Form.class) == null;
    }

    /**
     * Checks if this form has been submitted during the current request
     *
     * @return true if the form has been submitted during this request, false otherwise
     */
    public final boolean isSubmitted()
    {
        return getFlag(FLAG_SUBMITTED);
    }

    public boolean isVersioned()
    {
        return super.isVersioned();
    }

    protected boolean getStatelessHint()
    {
        return false;
    }

    protected String getInputNamePrefix()
    {
        return "";
    }

    public final IModel<T> getModel()
    {
        return (IModel<T>)getDefaultModel();
    }

    public final void setModel(IModel<T> model)
    {
        setDefaultModel(model);
    }

    public final T getModelObject()
    {
        return (T)getDefaultModelObject();
    }

    public final void setModelObject(T object)
    {
        setDefaultModelObject(object);
    }

    /**
     * @param component
     * @return The parent form for component
     */
    public static Form<?> findForm(Component component)
    {
        class FindFormVisitor implements Component.IVisitor<Form<?>>
        {
            Form<?> form = null;

            public Object component(Form<?> component)
            {
                form = component;
                return Component.IVisitor.STOP_TRAVERSAL;
            }
        }

        Form<?> form = component.findParent(Form.class);
        if (form == null)
        {
            // check whether the form is a child of a surrounding border
            Border border = component.findParent(Border.class);
            while ((form == null) && (border != null))
            {
                FindFormVisitor formVisitor = new FindFormVisitor();
                border.visitChildren(Form.class, formVisitor);
                form = formVisitor.form;
                if (form == null)
                {
                    border = border.findParent(Border.class);
                }
            }
        }
        return form;

    }

    /**
     * Produces javascript that registereds Wicket.Forms namespaces
     *
     * @param response
     */
    protected void registerJavascriptNamespaces(IHeaderResponse response)
    {
        response.renderJavascript(
                "if (typeof(Wicket)=='undefined') { Wicket={}; } if (typeof(Wicket.Forms)=='undefined') { Wicket.Forms={}; }",
                Form.class.getName());
    }

    /**
     * Utility method to assemble an id to distinct form components from diferent nesting levels.
     * Useful to generate input names attributes.
     *
     * @param component
     * @return form relative identification string
     */
    public static String getRootFormRelativeId(Component component)
    {
        String id = component.getId();
        final PrependingStringBuffer inputName = new PrependingStringBuffer(id.length());
        Component c = component;
        while (true)
        {
            inputName.prepend(id);
            c = c.getParent();
            if (c == null || (c instanceof Form<?> && ((Form<?>)c).isRootForm()) ||
                    c instanceof Page)
            {
                break;
            }
            inputName.prepend(Component.PATH_SEPARATOR);
            id = c.getId();
        }

        /*
         * having input name "submit" causes problems with JavaScript, so we create a unique string
         * to replace it by prepending a path separator, as this identification can be assigned to
         * an submit form component name
         */
        if (inputName.equals("submit"))
        {
            inputName.prepend(Component.PATH_SEPARATOR);
        }
        return inputName.toString();
    }

    /**
     * Gets the HTTP submit method that will appear in form markup. If no method is specified in the
     * template, "post" is the default. Note that the markup-declared HTTP method may not correspond
     * to the one actually used to submit the form; in an Ajax submit, for example, JavaScript event
     * handlers may submit the form with a "get" even when the form method is declared as "post."
     * Therefore this method should not be considered a guarantee of the HTTP method used, but a
     * value for the markup only. Override if you have a requirement to alter this behavior.
     *
     * @return the submit method specified in markup.
     */
    protected String getMethod()
    {
        String method = getMarkupAttributes().getString("method");
        return (method != null) ? method : METHOD_POST;
    }
}