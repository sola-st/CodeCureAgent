// New class to handle form validations
class FormValidator {
    private Form<?> form;

    public FormValidator(Form<?> form) {
        this.form = form;
    }

    public void validate() {
        if (form.isEnabledInHierarchy() && form.isVisibleInHierarchy()) {
            validateComponents();
            validateFormValidators();
            form.onValidate();
            validateNestedForms();
        }
    }

    protected final void validateComponents() {
        form.visitFormComponentsPostOrder(new ValidationVisitor()
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

    protected final void validateFormValidators() {
        final int count = form.formValidators_size();
        for (int i = 0; i < count; i++)
        {
            validateFormValidator(form.formValidators_get(i));
        }
    }

    protected final void validateFormValidator(final IFormValidator validator)
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
                else if (!form.isFormComponentVisibleInPage(dependent))
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("IFormValidator in form `" +
                                form.getPageRelativePath() +
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
            validator.validate(form);
        }
    }

    private void validateNestedForms() {
        form.visitChildren(Form.class, new IVisitor<Form<?>>()
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

// New class to handle form component model updates
class FormComponentModelUpdater {
    private Form<?> form;

    public FormComponentModelUpdater(Form<?> form) {
        this.form = form;
    }

    public void updateModels() {
        form.internalUpdateFormComponentModels();
        updateNestedFormComponentModels();
    }

    private final void updateNestedFormComponentModels() {
        form.visitChildren(Form.class, new IVisitor<Form<?>>()
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
}

// The Form class now delegates responsibility to the new classes
public abstract class Form<T> extends WebMarkupContainer implements IFormSubmitListener, IHeaderContributor {
    // ... (Other existing code and member variables remain unchanged)

    // Use the new classes within the Form methods
    private FormValidator formValidator;
    private FormComponentModelUpdater formComponentModelUpdater;

    public Form(final String id) {
        super(id);
        setOutputMarkupId(true);
        this.formValidator = new FormValidator(this);
        this.formComponentModelUpdater = new FormComponentModelUpdater(this);
    }

    protected final void validate() {
        formValidator.validate();
    }

    protected final void updateFormComponentModels() {
        formComponentModelUpdater.updateModels();
    }

    // ... (Other methods remain unchanged, but may also delegate to the new classes)
}